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

테스트(`@SpringBootTest`)는 실제 PostgreSQL에 연결하므로, 먼저 `docker compose up -d`를 실행해야 합니다.

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
└── payment/                  # 결제 도메인 (엔티티, 리포지토리)
src/main/resources/
├── application.yml           # 공통 설정
├── application-local.yml     # 로컬 개발용 (SQL 로그 등)
└── db/migration/             # Flyway 마이그레이션 (V{n}__{설명}.sql)
```

## 정리

```bash
docker compose down        # 컨테이너 중지
docker compose down -v     # 데이터 볼륨까지 삭제
```
