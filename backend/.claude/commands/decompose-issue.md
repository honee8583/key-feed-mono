# 작업 세분화하기
너는 이제부터 작업을 세분화하는 전략 전문가야. 아규먼트로 세분화해야 하는 작업을 입력받고 그 작업을 독립적인 여러 개의 이슈로 분해하는 작업을 해줘.

## 작업 순서
- **작업 분석:** 작업의 핵심 요구사항과 목표를 이해하기.
- **코드베이스 탐색:** 관련 코드를 탐색하여 수정이 필요한 파일과 패턴 파악하기.
- **작업 분해:** 주요 작업을 더 작고 관리하기 쉬운 하위 작업 또는 이슈로 나누기.
- **의존성 분석:** 다른 작업이 선행돼야 하는 의존성 파악하기.
- **분해된 이슈 출력:** 분해된 이슈들을 출력하기.
- **깃허브에 이슈를 생성할지 묻기:** 깃허브에 이 이슈들을 공식적으로 생성할지 여부를 사용자가 결정하도록 요청하기.

## 이슈 템플릿
<github_issue_template>
---
title: [종류] 명확하고 실행 가능한 제목
labels:
- type:feature|type:bug|type:refactor|type:test|type:documentation
- area: backend
- complexity:easy|complexity:medium|complexity:hard
---
## 설명
[무엇을 해야 하고 왜 해야 하는지]

## 완료 조건
- [ ] 특정 요구사항 1
- [ ] 특정 요구사항 2
- [ ] 단위 테스트 작성 및 통과
- [ ] 기존 테스트 통과
- [ ] README.md 업데이트 (API 변경 시)

## 구현 참고사항

### 수정할 파일
- `src/main/java/.../controller/XxxController.java` - [무엇을 변경할지]
- `src/main/java/.../service/impl/XxxServiceImpl.java` - [무엇을 변경할지]
- `src/main/java/.../dto/XxxRequestDto.java` - [무엇을 변경할지]
- `src/test/java/.../XxxServiceTest.java` - [테스트 추가]

### 따를 패턴
- 컨트롤러: `AuthController.java` 참조
- 서비스: `JoinServiceImpl.java` 참조
- DTO: `JoinRequestDto.java` 참조
- 테스트: `JoinServiceTest.java` 참조

### 기술적 고려사항
- [주의할 점 또는 특별 고려사항]
- [트랜잭션 처리]
- [예외 처리]
- [보안 고려사항]

## 의존성
- [ ] 없음
- [ ] 이슈 #[번호]가 먼저 완료되어야 함
- [ ] [외부 의존성] 필요
</github_issue_template>

## 도메인 구조 참고
```
src/main/java/com/leedahun/identityservice/
├── config/           # 전역 설정
├── common/           # 공통: 예외, 응답 래퍼, 유틸리티
├── domain/
│   ├── auth/         # 인증: User, JWT, 로그인/회원가입, 이메일 인증
│   ├── bookmark/     # 북마크 (폴더 구조)
│   ├── keyword/      # 키워드 (알림 토글)
│   └── source/       # RSS/피드 소스 관리
└── infra/client/     # OpenFeign 클라이언트
```

## 각 도메인의 레이어 구조
```
domain/
└── {도메인명}/
    ├── controller/       # REST API 엔드포인트
    ├── service/          # 인터페이스
    │   └── impl/         # 구현체
    ├── repository/       # JPA Repository
    ├── entity/           # JPA Entity
    ├── dto/              # 요청/응답 DTO
    ├── exception/        # 도메인 예외
    └── util/             # 도메인 유틸리티
```
