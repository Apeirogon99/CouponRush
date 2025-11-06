# Introduction

---

Coupon Rush는 선착순 쿠폰 발급 시스템입니다.

스파이크 테스트를 통해 서버의 병목 현상을 확인하고 이를 해결하고자 하였습니다.

## Build

---

- Java 17
- Spring Boot 3.5.6
- JDBC
- PostgresSQL 15
- Redis
- Kafka

--- 

## Installation

--- 

## Getting started

docker-compose.postgres.yml

docker-compose.redis.yml

docker-compose.kafka.yml

--- 

## Scenario

모든 테스트는 Docker Container에 작성

스팩
- postgres : 2core 2ram 
- redis    : 2core 2ram
- kafka    : 2core 2ram

--- 

### #1 Scenario - postgres
TPS ( 300 )

WithLock : 복잡한 검증 절차가 필요한 경우

TPS ( 1000 )

Where 절 : 단순히 쿠폰의 제고를 차감하고 발급하는 경우

[server] <---> [postgres]

### #2 Scenario - redis
TPS ( 1000 )

[server] <---> [redis] <---> [postgres]

### #3 Scenario - kafka
TPS ( 5000 )

---

## Summary

---

## 