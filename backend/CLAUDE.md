# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Key-Feed의 MSA 기반 마이크로서비스들을 단일 Spring Boot 모놀리틱 애플리케이션으로 통합한 프로젝트입니다.
키워드 기반 RSS 피드 구독 서비스로, RSS 소스에서 콘텐츠를 자동 수집해 개인화 피드/실시간 알림을 제공하고 토스페이먼츠 구독 결제를 지원합니다.
**Spring Boot 3.5.7 / Java 17 / MySQL / Redis**

---

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 로컬 프로파일로 실행 (필수)
./gradlew bootRun --args='--spring.profiles.active=local'

# 전체 테스트
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "com.keyfeed.keyfeedmonolithic.domain.auth.service.impl.LoginServiceImplTest"

# 단일 테스트 메서드
./gradlew test --tests "*.LoginServiceImplTest.로그인_성공"

# 클린 빌드
./gradlew clean build
```

## 환경 변수

로컬 실행 전 다음 환경 변수 또는 JVM 옵션이 필요합니다:

| 변수 | 설명 |
|------|------|
| `jwt_key` | JWT 서명용 시크릿 키 |
| `JASYPT_ENCRYPTOR_PASSWORD` | application.yml 암호화 프로퍼티(`ENC(...)`) 복호화 키 |

프로파일: `local` (로컬 개발), `prod` (프로덕션, 기본값)

---

## 아키텍처

`@EnableScheduling` 이 적용된 `KeyfeedMonolithicApplication` 이 진입점입니다.
최상위는 `domain` / `global` / `infra` 세 패키지로 나뉩니다.

### 도메인 모듈 구조

```
src/main/java/com/keyfeed/keyfeedmonolithic/
├── domain/
│   ├── auth/          # 인증/인가, JWT 발급, 이메일 인증, 비밀번호 재설정, 회원탈퇴
│   ├── bookmark/      # 북마크 및 폴더 관리
│   ├── content/       # 수집된 피드 콘텐츠 저장 (+ Outbox 발행)
│   ├── crawl/         # RSS 수집 엔진 (스케줄러 + Rome/Jsoup 파서)
│   ├── feed/          # 개인화 피드 읽기 API (커서 페이지네이션)
│   ├── keyword/       # 키워드 구독, 트렌딩 키워드, Redis 키워드 캐시
│   ├── notification/  # SSE 실시간 푸시 + 알림 이력
│   ├── payment/       # 토스 빌링키 기반 구독/결제/결제이력
│   └── source/        # RSS 소스 등록/유효성 검사(RSS·robots.txt·URL)
├── global/
│   ├── auth/          # jwt (JwtConstants, JwtProperties 등)
│   ├── client/        # toss (토스페이먼츠 HTTP 클라이언트 + config/dto)
│   ├── config/        # SecurityConfig, JpaAuditConfig, JasyptConfig, AsyncConfig 등
│   ├── constant/      # 공통 상수
│   ├── entity/        # BaseTimeEntity (createdAt/updatedAt 자동 관리)
│   ├── error/         # CustomException, GlobalExceptionHandler
│   ├── mail/          # EmailClient + 비동기 이메일 발송 이벤트
│   ├── message/       # SuccessMessage, ErrorMessage
│   ├── response/      # HttpResponse, CursorPage, CommonPageResponse 등
│   └── util/          # CursorPagination 등
└── infra/            # 이벤트/Outbox 파이프라인 (도메인 간 비동기 결합)
    ├── config/        # SchedulerConfig (전용 TaskScheduler 빈)
    ├── content/       # ContentOutboxPublisher / ContentOutboxConsumer
    ├── notification/  # NotificationConsumer + 멱등성 엔티티(NotificationProcessedContent)
    └── outbox/        # Outbox 엔티티/상태, RedisQueuePublisher, 소진 알림(OutboxAlertHandler)
```

### 도메인 내부 레이어 패턴

각 도메인은 기본적으로 아래 구조를 따릅니다 (도메인 특성상 일부는 가감됨):

```
domain-module/
├── controller/      # @RestController  (content/crawl 처럼 외부 API 없는 도메인은 없음)
├── service/         # 인터페이스
├── service/impl/    # 구현체 (@Service, @RequiredArgsConstructor)
├── repository/      # Spring Data JPA (+ 일부 Redis/JDBC 전용 리포지토리)
├── entity/          # @Entity (BaseTimeEntity 상속)  (feed/crawl 은 엔티티 없음)
├── dto/             # Request/Response DTO
└── exception/       # CustomException 상속 도메인 예외
```

도메인별 특이 구조:
- **payment**: 트랜잭션 경계 분리를 위해 `writer/`(SubscriptionWriter, PaymentHistoryWriter)와 `BillingExecutor`, `scheduler/`(BillingScheduler, SubscriptionExpiryScheduler)를 별도로 둡니다.
- **crawl**: `scheduler/CrawlScheduler`, `service/RssFeedParser`(Rome + Jsoup)로 구성되며 엔티티 없이 `source`/`content` 도메인을 사용합니다.
- **notification**: JPA 외에 벌크 insert용 `NotificationJdbcRepository`, 인메모리 `SseEmitterRepository` 를 함께 사용합니다.
- **keyword**: Redis Set 기반 `KeywordCacheRepository` + `KeywordCacheEvent(Listener)` 로 `keyword:users:{keyword}` 캐시를 동기화합니다.

---

## 핵심 파이프라인: RSS 수집 → 알림 (Outbox 패턴)

도메인 간 결합을 트랜잭션 밖으로 분리하기 위해 **Outbox + Redis 큐 + 멱등 소비자** 구조를 사용합니다.

1. `crawl/CrawlScheduler` (`@Scheduled(fixedDelay = 30분)`) → 최근 미수집 소스를 스레드풀로 병렬 크롤링
2. `crawl/CrawlService` → RSS 파싱 후 `Source.lastItemHash`(guid) 비교로 신규 항목만 추출
3. `content/ContentServiceImpl.saveContent` → `Content` + `Outbox` 행을 **한 트랜잭션**에 저장
4. `infra/content/ContentOutboxConsumer` (짧은 주기 폴링) → PENDING Outbox → Redis 큐 `queue:content.created` 로 LPUSH (재시도 + 소진 시 `OutboxExhaustedEvent` 알림)
5. `infra/notification/NotificationConsumer` → 큐 RPOP → 멱등성 체크(`NotificationProcessedContent`) → 구독자·키워드(Redis Set) 매칭 → `NotificationJdbcRepository` 벌크 insert
6. `notification/NotificationServiceImpl.send` → DB 저장 + 연결된 `SseEmitter` 로 실시간 푸시

전용 스케줄러 풀은 `infra/config/SchedulerConfig` 에 정의되어 있습니다.

---

## 주요 패턴 및 규칙

### 응답 형식
모든 API는 `HttpResponse`로 통일 (메시지는 `SuccessMessage`/`ErrorMessage` enum 사용):
```java
// global/response/HttpResponse.java
return new HttpResponse(200, SuccessMessage.XXX.getMessage(), data);
```

### 커스텀 예외
`CustomException`을 상속하여 도메인 예외를 정의하며, `GlobalExceptionHandler`(`@RestControllerAdvice`)에서 일괄 처리됩니다:
```java
// global/error/exception/CustomException.java
public abstract class CustomException extends RuntimeException {
    private final HttpStatus status;
}
```

### JWT 인증 흐름
- `domain/auth/filter/JwtAuthenticationFilter` 가 `UsernamePasswordAuthenticationFilter` 앞에 등록되어 `Authorization: Bearer <token>` 을 검증합니다 (Spring Security `STATELESS`).
- 인증된 사용자 ID는 컨트롤러에서 `@AuthenticationPrincipal Long userId` 로 주입됩니다.
- 액세스 토큰: 응답 바디, 리프레시 토큰: HTTP-only 쿠키. `/api/auth/refresh` 로 재발급.

### 페이지네이션
커서 기반 페이지네이션 사용:
- `CursorPage<T>` - 커서 페이지 응답 래퍼 (`global/response/CursorPage.java`)
- `CursorPagination` - 커서 추출 유틸리티 (`global/util/CursorPagination.java`)

### 토스페이먼츠 연동
- `global/client/toss/TossPaymentsClient`(`RestTemplate` 기반)로 빌링키 발급/결제/취소/조회를 수행합니다.
- 토스 시크릿 키는 `application.yml` 에 Jasypt `ENC(...)` 로 암호화되어 있습니다.
- `toss.api.duration` Micrometer 타이머로 호출 지표를 수집합니다.

### 주석
- 코드에 주석을 작성하지 않습니다.

---

## 로컬 개발 환경

```yaml
# application-local.yml 기준 (env/gitignore 로 관리되어 저장소에는 없음)
MySQL: localhost:3306/test (username: root, password: 1111)
Redis: localhost:6379
JWT 만료: 로컬은 길게 설정
SQL 로깅: P6Spy
```

DB 스키마는 `hibernate.ddl-auto: validate` + **Flyway** 로 관리합니다. 마이그레이션은 `src/main/resources/db/migration/V<n>__<설명>.sql` 형식이며, 브랜치 병렬 작업 시 **버전 번호 중복에 주의**하세요.

---

## 보안 설정

**Public 엔드포인트** (인증 불필요, `SecurityConfig` 기준):
- `/api/auth/**` - 로그인, 회원가입, 이메일 인증, 비밀번호 재설정
- `/api/keywords/trending` - 트렌딩 키워드 조회
- `/internal/**` - 내부 통신용으로 permitAll 예약 (현재 구현된 컨트롤러는 없음)
- `/actuator/**` - 헬스 체크/메트릭

**인증 필요**: `/api/keywords/**`, `/api/sources/**`, `/api/bookmarks/**`, `/api/users/**`, `/api/payment-methods/**`, `/api/subscriptions/**`, `/api/payment-history/**` (그 외 모든 요청 `authenticated`)
