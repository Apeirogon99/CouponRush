# CouponRush

선착순 쿠폰 발급 시스템

동일한 비즈니스 로직을 3가지 아키텍처로 구현하여 동시성 제어 방식에 따른 성능 특성을 비교한다.

> "동시에 3,000명이 1,000장의 쿠폰을 요청할 때, 아키텍처에 따라 정확성, 응답 속도, 처리량이 어떻게 달라지는가?"

## 시나리오 비교 결과

3,000명 동시 요청, 1,000장 한정, 10초 스파이크 테스트 (로컬 환경 기준):

| 지표 | Scenario 1 (DB) | Scenario 2 (DB+Redis) | Scenario 3 (DB+Redis+Kafka) |
|------|:---:|:---:|:---------------------------:|
| **발급 정확성** | 1,000/1,000 | 1,000/1,000 |         1,000/1,000         |
| **에러율** | 0% | 0% |             0%              |
| **총 처리 요청** | 4,390 | 10,425 |           10,190            |
| **처리량 (req/s)** | 147 | 425 |             404             |
| **평균 응답 시간** | 11.34s | 3.53s |            3.44s            |
| **p95 응답 시간** | 15.39s | 5.81s |            6.32s            |
| **성공 응답(200) 평균** | 5.4s | 2.76s |            559ms            |

**핵심 개선 포인트:**

- **Scenario 1 → 2:**
  - Redis 도입으로 처리량 **3배** 향상 (147 → 425 req/s)
  - DB 락 대기가 제거되면서 대부분의 요청을 빠르게 처리
- **Scenario 2 → 3:**
  - 성공 응답 속도 **5배** 향상 (2.76s → 559ms)
  - Kafka가 DB 쓰기를 API 스레드에서 완전히 분리하여 사용자 체감 응답 시간이 크게 개선

---

## Scenario 1: RDBMS 병목 분석

### 문제 발견

락 없이 `SELECT` → `UPDATE`를 수행하면 TOCTOU(Time-of-Check to Time-of-Use) Race Condition이 발생한다.

```
시간  Thread A                    Thread B                    DB (잔여: 1장)
─────────────────────────────────────────────────────────────────────────────
 t1   SELECT remaining → 1장
 t2                               SELECT remaining → 1장
 t3   remaining > 0 → 발급!
 t4   UPDATE remaining = 0
 t5                               remaining > 0 → 발급!       ← 이미 0장인데 통과
 t6                               UPDATE remaining = -1       ← 초과 발급
```

K6 스파이크 테스트 결과 **초과 발급, 중복 발급, 데이터 정합성 붕괴**가 동시에 발생했다.

### 해결 방안 검토

| 방안 | 장점 | 문제점 |
|------|------|--------|
| UNIQUE 제약조건 | 중복 발급 방지 가능 | 초과 발급은 별도 처리 필요 |
| 낙관적 락 (Optimistic Lock) | 읽기 성능 유지 | 충돌 시 재시도 필요, 스파이크 상황에서 재시도 비용 폭증 |
| **비관적 락 (Pessimistic Lock)** | **조회-검증-갱신을 직렬화하여 Race Condition 원천 차단** | **처리량 저하, DB 부하 집중** |

초기에 낙관적 락을 구현하여 테스트했으나 3,000명 동시 요청 스파이크에서는 충돌률이 극도로 높아 재시도가 연쇄적으로 발생했다.

재시도 비용이 감당할 수 없는 수준이었기에, 정합성을 가장 확실하게 보장하는 비관적 락으로 전환했다.

### 적용: `SELECT ... FOR UPDATE`

`SELECT ... FOR UPDATE`로 쿠폰 행에 배타적 락을 걸어, 한 트랜잭션이 완료될 때까지 다른 트랜잭션의 접근을 차단한다.

```
Client ──POST──→ [Spring Boot]
                   │
                   ├─ BEGIN TRANSACTION
                   ├─ SELECT ... FOR UPDATE (락 획득, 대기)
                   ├─ 중복 발급 확인
                   ├─ UPDATE issued_quantity + 1
                   ├─ INSERT issued_coupon
                   ├─ COMMIT (락 해제)
                   │
                 ←─── 200 OK
```

### 테스트 결과

| 지표 | 값 |
|------|:---:|
| 발급 정확성 | 1,000/1,000 |
| 처리량 | 147 req/s |
| 평균 응답 시간 | 11.34s |
| p95 응답 시간 | 15.39s |

MySQL 2cpu/2ram 기준 TPS 약 147. 정합성은 완벽하게 보장되었다.

### 남은 병목

- 모든 요청이 **DB 단일 행의 락을 순차적으로 대기** → 처리량 병목
- 트랜잭션 내에서 락 획득 → 검증 → 갱신 → 저장을 모두 수행 → 락 보유 시간이 길어짐
- 재고가 소진된 이후에도 락을 잡아야 "매진"을 알 수 있음 → 불필요한 대기

---

## Scenario 2: Redis 도입으로 DB 병목 해소

### 이전 문제

Scenario 1의 핵심 병목은 **모든 동시성 제어와 데이터 저장이 DB에 집중**된 것이었다.

재고 확인, 재고 차감, 발급 기록 저장이 하나의 트랜잭션에 묶여 락 보유 시간이 길었고 재고가 소진된 뒤에도 락을 획득해야 매진 여부를 확인할 수 있었다.

### 개선 방향

두 가지 병목을 분리하여 해결한다:

**1. 락 없이 사전검증 (Fast-Fail)**

기존에는 재고 소진 여부를 락을 잡아야 알 수 있었다.

Redis 도입 후 분산락을 잡기 전에 `GET`과 `SISMEMBER`로 재고 소진과 중복 발급을 먼저 검증할 수 있게 되었다.

3,000명 중 이미 매진된 이후의 요청은 락 경합 없이 즉시 반려된다.

**2. DB 쓰기를 비동기로 분리**

DB에 쓰는 비용이 락 보유 시간을 늘리는 주요 원인이었다.

Redis에서 재고 차감과 발급 기록을 처리한 뒤, DB 저장은 `@Async`로 별도 스레드에서 실행하여 API 응답 경로에서 DB I/O를 제거했다.

### 적용

- **Redis** -- 재고 확인(`GET`), 중복 검사(`SISMEMBER`), 재고 차감(`DECR`), 발급 기록(`SADD`). 인메모리 연산으로 마이크로초 단위 처리.
- **Redisson 분산락** -- 사용자별 락으로 동일 사용자의 동시 요청만 직렬화. DB 행 락과 달리 다른 사용자의 요청은 병렬 처리.
- **`@Async` DB 저장** -- 발급 성공 응답 후 별도 스레드에서 DB에 기록. 실패 시 Redis 보상(`INCR` + `SREM`).

```
Client ──POST──→ [Spring Boot]
                   │
                   ├─ Redis SISMEMBER (중복 사전검증)     ─┐
                   ├─ Redis GET (재고 사전검증)            ─┤ 락 없이 Fast-Fail
                   │                                       ─┘
                   ├─ Redisson tryLock (분산락 획득)
                   │   ├─ Redis SISMEMBER (중복 재확인)
                   │   ├─ Redis DECR (원자적 재고 차감)
                   │   └─ Redis SADD (발급 기록)
                   ├─ unlock
                   │
                 ←─── 200 OK (즉시 응답)
                   │
                   └─ @Async ──→ [DB]
                                  ├─ UPDATE issued_quantity
                                  └─ INSERT issued_coupon
                                  (실패 시 Redis 보상: INCR + SREM)
```

### 테스트 결과

| 지표 | 값 | 변화 |
|------|:---:|:---:|
| 발급 정확성 | 1,000/1,000 | - |
| 처리량 | 425 req/s | +189% |
| 평균 응답 시간 | 3.53s | -69% |
| 성공 응답 평균 | 2.76s | -49% |

Redis 1cpu/1ram 추가 투입에도 DB 비관적 락의 직렬 처리가 인메모리 연산으로 대체되면서 TPS가 2배 이상 증가했다.

### 트레이드오프

Redis를 도입하면서 **두 가지 새로운 과제**가 생겼다:

- **Redis-DB 정합성 유지:** 비동기 DB 저장 실패 시 Redis 보상 처리(재고 복구 `INCR` + 발급 기록 제거 `SREM`)가 필요.
  - 복잡성이 증가하지만, 처리량 개선 폭이 이를 충분히 상쇄한다.
- **`@Async` 스레드풀 포화:** 스레드풀(`core=20, max=50, queue=500`)이 스파이크 시 포화되면 `CallerRunsPolicy`에 의해 API 스레드가 직접 DB 쓰기를 수행한다.
  - 데이터 유실은 방지되지만, 성공 응답 시간이 느려지는 원인이 된다.

---

## Scenario 3: Kafka 도입으로 API 스레드에서 DB 완전 분리

### 이전 문제

Scenario 2의 `@Async`는 스레드풀 기반이므로, 스파이크 상황에서 **스레드풀이 포화되면 `CallerRunsPolicy`에 의해 API 스레드가 직접 DB 쓰기를 수행**하는 문제가 있었다.

`AsyncConfig`의 스레드풀 설정은 `corePoolSize=20`, `maxPoolSize=50`, `queueCapacity=500`이다. 

스파이크 시 1,000건의 성공 요청이 짧은 시간에 집중되면, 큐(500) + 최대 스레드(50)를 초과하는 요청이 발생한다.

이때 `CallerRunsPolicy`가 작동하여 API 스레드에서 DB INSERT/UPDATE를 동기 실행하게 된다.

이것이 성공 응답 평균 2.76s의 주요 원인이다.

### 개선 방향

`@Async` 스레드풀을 Kafka 메시지 큐로 대체하여, API 스레드에서 DB I/O를 완전히 제거한다:

- **스레드풀 포화 문제 제거** -- Kafka `send()`는 내부 버퍼에 적재 후 즉시 반환.
  - 스레드풀 크기와 무관하게 항상 논블로킹
- **수동 커밋** -- `AckMode.RECORD`로 메시지 단위 수동 커밋
  - Consumer가 DB 저장을 완료해야 커밋

### 적용

```
Client ──POST──→ [Spring Boot API]
                   │
                   ├─ Redis SISMEMBER (중복 사전검증)
                   ├─ Redis GET (재고 사전검증)
                   ├─ Redisson tryLock (분산락 획득)
                   │   ├─ Redis SISMEMBER (중복 재확인)
                   │   ├─ Redis DECR (원자적 재고 차감)
                   │   └─ Redis SADD (발급 기록)
                   ├─ unlock
                   ├─ Kafka send (비동기 publish)
                   │
                 ←─── 200 OK (즉시 응답, DB 접근 없음)


                 [Kafka Consumer]
                   │
                   ├─ 이벤트 수신
                   ├─ UPDATE issued_quantity
                   ├─ INSERT issued_coupon
                   └─ ACK (수동 커밋)
                   (실패 시 Redis 보상: INCR + SREM)
```

### 테스트 결과

| 지표 | 값 | 변화 (vs Scenario 2) |
|------|:---:|:---:|
| 발급 정확성 | 1,000/1,000 | - |
| 처리량 | 404 req/s | 유사 |
| 평균 응답 시간 | 3.44s | 유사 |
| 성공 응답 평균 | **559ms** | **-80%** |

**처리량이 유사한 이유:** Scenario 2와 3 모두 API 스레드의 병목은 Redis 연산(6회 왕복 + 분산락 대기)이다.

`@Async`와 `kafkaTemplate.send()` 모두 정상 상태에서는 논블로킹으로 즉시 반환하므로 비동기 전달 방식의 차이는 전체 처리량에 영향을 주지 않는다.

**성공 응답이 빨라진 이유:** Scenario 2의 스레드풀(`core=20, max=50, queue=500`)은 스파이크 시 1,000건의 성공 요청이 짧은 시간에 몰리면 포화된다.

이때 `CallerRunsPolicy`가 작동하여 일부 성공 요청의 API 스레드가 직접 DB INSERT/UPDATE를 수행하게 되고, 이 DB I/O 시간이 응답에 포함된다. 

Kafka는 스레드풀을 사용하지 않으므로 이 문제가 발생하지 않기에 성공 응답에서만 80% 개선(2.76s → 559ms)이 나타난다.

**Eventual Consistency:** teardown 시점에 DB 발급 수가 1,000 미만으로 보일 수 있으나 Kafka Consumer가 비동기 처리를 완료하면 정확히 1,000개가 DB에 기록된다.

---

## 실행 방법

### 사전 요구사항

- Java 17
- Docker Desktop
- K6 (`k6 version`으로 설치 확인)

### 공통 준비

시나리오 전환 시 기존 컨테이너와 볼륨을 정리한다:

```bash
docker-compose --profile scenario1 --profile scenario2 --profile scenario3 down -v
```

### Scenario 1: DB only

```bash
# 인프라 (MySQL)
docker-compose --profile scenario1 up -d

# 앱 시작
./gradlew bootRun --args='--spring.profiles.active=scenario1'

# K6 부하 테스트 (별도 터미널)
k6 run k6/load-test.js
```

### Scenario 2: DB + Redis

```bash
# 인프라 (MySQL + Redis)
docker-compose --profile scenario2 up -d

# 앱 시작
./gradlew bootRun --args='--spring.profiles.active=scenario2'

# K6 부하 테스트
k6 run k6/load-test.js
```

### Scenario 3: DB + Redis + Kafka

```bash
# 인프라 (MySQL + Redis + Zookeeper + Kafka)
docker-compose --profile scenario3 up -d
# Kafka 안정화까지 약 15초 대기

# 앱 시작
./gradlew bootRun --args='--spring.profiles.active=scenario3'

# K6 부하 테스트
k6 run k6/load-test.js
```

### 주의사항

- 시나리오 전환 시 반드시 `docker-compose down -v`로 볼륨 포함 정리
- Kafka 리스너: Docker 내부 `kafka:9092` / 로컬 `localhost:29092`로 분리. 로컬 `bootRun`은 29092 포트로 접근

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.6 |
| Persistence | JDBC (JdbcTemplate, ORM 없음) |
| Database | MySQL 8 |
| Cache / Lock | Redis 7, Redisson |
| Messaging | Kafka (Confluent 7.5.0) |
| Test | JUnit 5, Testcontainers |
| Load Test | K6 |
| Infra | Docker Compose |

## 프로젝트 구조

```
com.apeirogon.rush
├── api
│   ├── controller        # REST 엔드포인트 (CouponController)
│   └── config            # Spring 설정 (DataSource, Redis, Kafka, Async)
├── domain                # 도메인 레코드 (Coupon, IssuedCoupon)
├── storage               # JDBC 리포지토리 (raw SQL)
├── strategy              # Strategy 패턴으로 시나리오 분기
│   ├── PessimisticLockStrategy   # scenario1: DB 비관적 락
│   ├── DistributedStrategy       # scenario2: Redis + @Async
│   └── MessagingStrategy         # scenario3: Redis + Kafka
├── async                 # AsyncCouponSaver (@Async DB 저장)
├── messaging             # Kafka Producer/Consumer
└── support               # 에러 타입, API 응답 래퍼
```

## API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/coupons` | 쿠폰 생성 (수량 지정) |
| `POST` | `/coupons/{couponId}/issues` | 쿠폰 발급 요청 (`{ userId }`) |
| `GET` | `/coupons` | 쿠폰 목록 조회 |
