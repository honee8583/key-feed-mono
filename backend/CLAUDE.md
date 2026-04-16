# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Key-Feed의 MSA 기반 마이크로서비스들을 단일 Spring Boot 모놀리틱 애플리케이션으로 통합한 프로젝트입니다.
**Spring Boot 3.5.7 / Java 17 / MySQL**

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
| `JASYPT_ENCRYPTOR_PASSWORD` | application.yml 암호화 프로퍼티 복호화 키 |

프로파일: `local` (로컬 개발), `prod` (프로덕션, 기본값)

---

## 아키텍처

### 도메인 모듈 구조

```
src/main/java/com/keyfeed/keyfeedmonolithic/
├── domain/
│   ├── auth/        # 인증/인가, JWT 발급, 이메일 인증, 비밀번호 재설정
│   ├── bookmark/    # 북마크 및 폴더 관리
│   ├── content/     # 피드 콘텐츠
│   ├── keyword/     # 키워드 구독, 트렌딩 키워드
│   └── source/      # RSS 소스 관리, 유효성 검사
└── global/
    ├── config/      # SecurityConfig, JpaAuditConfig, JasyptConfig 등
    ├── auth/jwt/    # JwtConstants, JwtProperties
    ├── entity/      # BaseTimeEntity (createdAt/updatedAt 자동 관리)
    ├── error/       # CustomException, GlobalExceptionHandler
    ├── response/    # HttpResponse, CursorPage, CommonPageResponse
    └── util/        # CursorPagination
```

### 도메인 내부 레이어 패턴

각 도메인은 동일한 구조를 따릅니다:

```
domain-module/
├── controller/      # @RestController
├── service/         # 인터페이스
├── service/impl/    # 구현체 (@Service, @RequiredArgsConstructor)
├── repository/      # Spring Data JPA
├── entity/          # @Entity (BaseTimeEntity 상속)
├── dto/             # Request/Response DTO
└── exception/       # CustomException 상속 도메인 예외
```

---

## 주요 패턴 및 규칙

### 응답 형식
모든 API는 `HttpResponse`로 통일:
```java
// global/response/HttpResponse.java
return new HttpResponse(200, SuccessMessage.XXX.getMessage(), data);
```

### 커스텀 예외
`CustomException`을 상속하여 도메인 예외를 정의하며, `GlobalExceptionHandler`에서 일괄 처리됩니다:
```java
// global/error/exception/CustomException.java
public abstract class CustomException extends RuntimeException {
    private final HttpStatus status;
}
```

### JWT 인증 흐름
- `GatewayHeaderAuthenticationFilter` → HTTP 헤더(`X-User-Id`, `X-User-Roles`)에서 사용자 정보 추출
- 컨트롤러에서 `@AuthenticationPrincipal Long userId`로 사용자 ID 주입
- 액세스 토큰: 응답 바디, 리프레시 토큰: HTTP-only 쿠키

### 페이지네이션
커서 기반 페이지네이션 사용:
- `CursorPage<T>` - 커서 페이지 응답 래퍼 (`global/response/CursorPage.java`)
- `CursorPagination` - 커서 추출 유틸리티 (`global/util/CursorPagination.java`)

### 내부 API
`/internal/**` 엔드포인트는 인증 없이 접근 가능 (서비스 내부 통신용):
- `BookmarkInternalController`, `KeywordInternalController`, `SourceInternalController`

---

## 로컬 개발 환경

```yaml
# application-local.yml 기준
MySQL: localhost:3306/test (username: root, password: 1111)
JWT 만료: 14일 (로컬은 길게 설정됨)
SQL 로깅: P6Spy (p6spy: info 레벨)
```

---

## 보안 설정

**Public 엔드포인트** (인증 불필요):
- `/api/auth/**` - 로그인, 회원가입, 이메일 인증, 비밀번호 재설정
- `/internal/**` - 내부 서비스 통신
- `/actuator/**` - 헬스 체크

**인증 필요**: `/api/keywords/**`, `/api/sources/**`, `/api/bookmarks/**`, `/api/users/**`
