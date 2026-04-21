# Key-Feed

키워드 기반 RSS 피드 구독 서비스입니다. 사용자가 관심 키워드를 등록하면 관련 RSS 소스에서 콘텐츠를 자동 수집하여 개인화된 피드를 제공합니다.

## 프로젝트 구조

```
key-feed-mono/
├── backend/    # Spring Boot 모놀리틱 백엔드
├── frontend/   # React + TypeScript 프론트엔드
├── k6/         # 부하 테스트 스크립트
└── API_SPEC.md # API 명세서
```

## 기술 스택

### Backend
- **Java 17** / **Spring Boot 3.5.7**
- Spring Security (JWT 인증)
- Spring Data JPA / MySQL
- Redis (캐싱, 토큰 관리)
- Jasypt (설정값 암호화)
- Micrometer + Prometheus (메트릭 수집)
- Rome + Jsoup (RSS 파싱 및 크롤링)

### Frontend
- **React 19** / **TypeScript 5.9**
- Vite, Tailwind CSS
- TanStack Query, Zustand
- React Hook Form + Zod
- Toss Payments SDK (결제)

### Infra
- GitHub Actions (CI/CD, Blue-Green 무중단 배포)
- Nginx (리버스 프록시, Blue-Green 라우팅)
- Docker

## 주요 기능

- **키워드 구독**: 관심 키워드 등록 및 관련 콘텐츠 자동 수집
- **RSS 소스 관리**: RSS URL 등록, 유효성 검증, 추천 소스 제공
- **개인화 피드**: 구독 키워드 기반 커서 페이지네이션 피드
- **북마크**: 폴더 기반 콘텐츠 저장 및 관리
- **실시간 알림**: SSE 기반 새 콘텐츠 알림
- **구독 결제**: 토스페이먼츠 빌링키 기반 자동결제 및 구독 관리
- **이메일 인증**: 회원가입 및 비밀번호 재설정 시 이메일 인증

## 로컬 실행

### Backend

환경 변수 설정:

| 변수 | 설명 |
|------|------|
| `jwt_key` | JWT 서명용 시크릿 키 |
| `jasypt_key` | application.yml 암호화 프로퍼티 복호화 키 |

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

로컬 기본 설정 (application-local.yml):
- MySQL: `localhost:3306/test` (root / 1111)
- Redis: `localhost:6379`
- 서버 포트: `8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## 빌드

```bash
# Backend
cd backend
./gradlew build

# Frontend
cd frontend
npm run build
```

## 테스트

```bash
cd backend

# 전체 테스트
./gradlew test

# 단일 클래스
./gradlew test --tests "com.keyfeed.keyfeedmonolithic.domain.auth.service.impl.LoginServiceImplTest"

# 단일 메서드
./gradlew test --tests "*.LoginServiceImplTest.로그인_성공"
```

## API 명세

[API_SPEC.md](./API_SPEC.md) 참고
