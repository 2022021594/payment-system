# Payment System

Java 25 + Spring Boot 4.1 + Gradle + PostgreSQL 기반 결제 시스템.

## 기술 스택

| 구분 | 버전 |
|---|---|
| Java | 25 (Temurin) |
| Spring Boot | 4.1.1 |
| Gradle | 9.8.0 (Wrapper) |
| PostgreSQL | 17 (Docker) |
| DB 마이그레이션 | Flyway |

## 사전 요구사항

- JDK 25
- Docker / Docker Compose

Gradle은 설치할 필요 없습니다. 저장소에 포함된 Wrapper(`gradlew`)를 사용합니다.

## 실행 방법

```bash
# 1. PostgreSQL 기동 (호스트 포트 5433)
docker compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'      # macOS/Linux
.\gradlew.bat bootRun --args='--spring.profiles.active=local'  # Windows

# 3. 헬스 체크
curl http://localhost:8080/actuator/health
```

> DB 포트를 5432가 아닌 **5433**으로 노출합니다. 로컬에 설치된 PostgreSQL과 충돌하지 않게 하기 위해서입니다.
> 바꾸려면 `DB_PORT` 환경변수를 docker compose와 앱 양쪽에 똑같이 지정하세요.

## 빌드 & 테스트

```bash
./gradlew build
```

테스트는 Testcontainers가 PostgreSQL 컨테이너를 자동으로 띄우므로 `docker compose`가 없어도 됩니다. Docker만 실행 중이면 됩니다.

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_HOST` | `localhost` | DB 호스트 |
| `DB_PORT` | `5433` | DB 포트 |
| `DB_NAME` | `payment` | DB 이름 |
| `DB_USERNAME` | `payment` | DB 사용자 |
| `DB_PASSWORD` | `payment` | DB 비밀번호 |
| `SERVER_PORT` | `8080` | 애플리케이션 포트 |

## 프로젝트 구조

```
src/main/java/com/example/payment/
├── PaymentSystemApplication.java
└── payment/                  # 결제 도메인 (엔티티, 리포지토리, 서비스, 컨트롤러, 예외 처리)
src/main/resources/
├── application.yml           # 공통 설정
├── application-local.yml     # 로컬 개발용 (SQL 로그 등)
└── db/migration/             # Flyway 마이그레이션 (V{n}__{설명}.sql)
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/payments` | 결제 생성 (READY) |
| GET | `/payments/{id}` | 결제 조회 |
| GET | `/payments?orderId=` | 주문 ID로 결제 조회 |
| POST | `/payments/{id}/approve` | 결제 승인 (READY → APPROVED/FAILED) |
| POST | `/payments/{id}/cancel` | 결제 전액 취소 (APPROVED → CANCELED) |

취소는 PG가 취소에 성공했을 때만 CANCELED로 바뀝니다. PG가 거절하면 상태는 그대로 두고 422로 응답하며, 승인 상태가 아닌 결제(이미 취소된 결제 포함)를 취소하면 409로 응답합니다.

## 추후 계획

- **부분 취소**: 현재는 전액 취소만 지원합니다. 부분 취소를 하려면 누적 취소 금액 컬럼(Flyway 마이그레이션), 취소 요청 금액 검증(잔액 이내), 취소 이력 테이블이 필요하고, `PaymentGateway.cancel`에도 취소 금액이 전달되어야 합니다.

## 알려진 한계

- **PG 취소 성공 후 DB 저장 실패 (정합성 문제)**: PG 취소는 성공했는데 우리 DB의 상태 변경(CANCELED) 저장이 실패하면, PG에서는 취소됐지만 우리 DB에는 APPROVED로 남습니다. 현재는 보상 처리나 재시도, 대사(reconciliation)를 구현하지 않았습니다. 승인도 같은 구조입니다.
- **PG 호출 중 행 잠금 유지**: 승인·취소는 결제 행을 잠근 채 PG를 호출합니다. 중복 처리는 막지만, 실제 PG처럼 응답이 느리면 DB 연결과 잠금을 오래 점유합니다.
- **PG 거래 정보 미저장**: PG 거래 ID와 거절 사유를 DB에 저장하지 않습니다.
- **Fake PG**: `FakePaymentGateway`가 연동되어 있습니다. 승인은 1,000만 원 이하만 허용하고 취소는 항상 성공합니다.

## 정리

```bash
docker compose down        # 컨테이너 중지
docker compose down -v     # 데이터 볼륨까지 삭제
```
