# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

Java 25 + Spring Boot 4.1.1 + Gradle 9.8.0(Wrapper) + PostgreSQL 17 기반 결제 시스템.
베이스 패키지: `com.example.payment`

## 주요 명령어

```bash
docker compose up -d                                      # PostgreSQL 기동 (호스트 포트 5433)
./gradlew build                                           # 빌드 + 테스트 (DB가 떠 있어야 함)
./gradlew test --tests "com.example.payment.SomeTest"     # 단일 테스트
./gradlew bootRun --args='--spring.profiles.active=local' # 로컬 실행
```

Windows에서는 `.\gradlew.bat`을 사용합니다. 로컬에 Gradle이 설치되어 있지 않으므로 항상 Wrapper를 사용하세요.

## 규칙

- **스키마 변경은 Flyway로만 합니다.** `src/main/resources/db/migration/V{n}__{설명}.sql` 파일을 새로 추가하고, 이미 적용된 마이그레이션 파일은 수정하지 않습니다. JPA는 `ddl-auto: validate`라서 엔티티와 스키마가 다르면 기동에 실패합니다.
- **금액은 `BigDecimal` / `NUMERIC(19,2)`를 사용합니다.** `double`/`float`는 쓰지 않습니다.
- **시간은 UTC로 저장합니다** (`TIMESTAMPTZ`, `OffsetDateTime`, `hibernate.jdbc.time_zone=UTC`).
- 엔티티에서 enum은 `@Enumerated(EnumType.STRING)`으로 저장합니다.
- `spring.jpa.open-in-view=false`이므로 트랜잭션 경계는 서비스 계층에서 관리합니다.
- 도메인별로 패키지를 나눕니다 (예: `com.example.payment.payment`).
- DB 접속 정보는 환경변수(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`)로 덮어씁니다. 비밀값은 커밋하지 않습니다.

## 환경 관련 주의사항

- 개발 머신에 로컬 PostgreSQL이 5432 포트를 쓰고 있어서, Docker DB는 **5433**으로 노출합니다.
- Spring Boot 4는 스타터가 모듈별로 분리되어 있습니다 (예: `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, 테스트용 `*-test` 스타터). Boot 3 시절 이름(`spring-boot-starter-web`)과 헷갈리지 마세요.
