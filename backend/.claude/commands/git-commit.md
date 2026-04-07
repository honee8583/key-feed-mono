---
description: 백엔드 변경사항을 컨벤션 기반 커밋합니다
args: [message]
allowed-tools: ["Bash(git*)"]
---
$ARGUMENTS 커밋 메시지로 다음을 확인하고 최적화된 Conventional Commit 형식으로 재작성하세요:

## 커밋 메시지 형식
`type: 한국어 설명`

## 변경 범주 선택 (하나)
- `feat`: 새로운 기능 (API 엔드포인트, 페이지, 모달 등)
- `fix`: 버그 수정 (쿼리 오류, 로직 수정 등)
- `refactor`: 코드 리팩토링 (중복 제거, 메서드 추출, 패키지 구조 정리)
- `test`: 테스트 추가/수정 (JUnit, Mockito, WireMock)
- `docs`: 문서 추가/수정 (README, CLAUDE.md)
- `chore`: 빌드/의존성/설정 (build.gradle, Gateway 설정, Docker)
- `ci`: CI/CD 파이프라인 (GitHub Actions)
- `perf`: 성능 최적화 (쿼리 최적화, 캐싱, 인덱싱)

## 규칙
- backend 폴더의 변경사항을 커밋
- 적절한 커밋 단위로 커밋
- 제목은 **한국어**로 작성
- backend/frontend 구분을 위해 커밋 타입(backend/frontend) 표시 ex) feat(backend)
- 제목은 50자 이내로 간결하게
- "~구현", "~추가", "~수정", "~개선" 등 동사형 종결
- 여러 변경이 있을 경우 "및"으로 연결 (최대 2개)
- 클로드 코드 명시는 제거("Co-Authored-By: ~~" 의 마지막 문장은 제거)
- 클로드 코드 관련 내용은 커밋 메시지에서 제거

## 형식 예시
```
feat: 비밀번호 재설정 기능 구현
fix: 파싱가능검사 로직에 User-Agent 설정 추가
refactor: 중복 변환 로직을 헬퍼 메서드로 추출
test: PasswordResetService 단위 테스트 작성
chore: Gateway 라우팅 설정 추가
```

## 커밋 절차
1. `git status .`로 변경사항 확인
2. `git diff --staged`로 스테이징된 변경 내용 확인
3. 변경 내용을 분석하여 적절한 type과 한국어 설명 결정
4. `git add`로 관련 파일만 스테이징 (민감 파일 제외)
5. 커밋 실행
