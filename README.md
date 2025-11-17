# Introduction

---

Coupon Rush는 선착순 쿠폰 발급 시스템입니다. </br>
스파이크 테스트를 통해 서버의 병목 현상을 확인하고 이를 해결하고자 하였습니다. </br>

시나리오는 3개입니다. </br>
RDBMS -> REDIS -> KAFKA 순으로 하나씩 적용하여 K6로 스파이크 테스트를 진행합니다.</br>

시나리오는 병목현상과 트러블 슈팅을 통해 TPS 증가량에 대해 작성하였습니다. </br>
# Summary

---
## Scenario #1 - DB
> ---
> ### 요약
> 
> TPS 증가량 : TPS 100 -> 300 (300%) </br> 
> 병목 지점 : </br>
> ---
> ### 트러블 슈팅
> 
> DB Lock 병목지점 발생 어떻게 해결하여 100 에서 300 늘림 </br>
> dd
> ---

## Scenario #2 - DB + REDIS
> ---
> ### 요약
> 
> TPS 증가량 : TPS 300 -> 1000 (333%) </br>
> 병목 지점 : </br>
> ---
> ### 트러블 슈팅
> 
> 기존 어떤 문제점을 해결하기 위해 redis도입 </br>
> 대기열 도입하여 어떤 문제를 해결함 </br>
> Redisson 사용하여 분산락을 통해 락 해결
> ---

## Scenario #3 - DB + REDIS + KAFKA
> ---
> ### 요약
> 
> TPS 증가량 : TPS 1000 -> 5000 (500%) </br>
> 병목 지점 : </br>
> ---
> ### 트러블 슈팅
> 
> 기존 어떤 문제점을 해결하기 위해 KAFKA를 도입 </br>
> 메세지 브로커 시스템을 통해 어쩌구 저쩌구
> ---

---

## Getting started

루트 폴더에서 시나리오별로 테스트 결과를 확인할 수 있습니다. </br>
**Docker나 IntelliJ 터미널에서 실행해주세요**
```
run-scenario 1
run-scenario 2
run-scenario 3
```

--- 

# Scenario

각각의 시나리오별로 K6를 통한 스파이크 부하테스트를 결과와 함께 정리하였습니다. </br>


## K6 Spike Scenario
[사진]

## #1 Scenario - db

### 아키텍쳐
[server] <---> [postgres]

### 부하테스트 결과 요약

### 병목 지점

### 병목 해결

TPS ( 300 )
WithLock : 복잡한 검증 절차가 필요한 경우
TPS ( 1000 )
Where 절 : 단순히 쿠폰의 제고를 차감하고 발급하는 경우

### #2 Scenario - db + redis

### 아키텍쳐
[server] <---> [redis] <---> [postgres]

### 부하테스트 결과 요약

### 병목 지점

### 병목 해결
RedisLock 느림
TPS ( 1000 )

### #3 Scenario - db + redis + kafka

### 아키텍쳐
[server] <---> [redis] <---> [postgres]

### 부하테스트 결과 요약

### 병목 지점

### 병목 해결
TPS ( 5000 )

---

# Build

---

## version
- Java 17
- Spring Boot 3.5.6
- JDBC
- PostgresSQL 15
- Redis 7
- Kafka 7.5.0

## Container deploy
- app      : 2core 2ram
- postgres : 2core 2ram
- redis    : 1core 1ram
- kafka    : 2core 2ram
