# CLAUDE.md — Frontend (key_feed_react)

이 문서는 `frontend/` 디렉터리에서 작업할 때 Claude가 따라야 할 지침입니다.

## 디자인 작업 지침

이 프로젝트의 화면 디자인은 Claude Design 프로젝트(`tracer`)를 기준으로 합니다.
디자인을 가져오거나 화면을 구현/수정할 때는 아래 지침을 따르세요.

```
Use the claude_design MCP (https://api.anthropic.com/v1/design/mcp, auth via /design-login) to import this project:
https://claude.ai/design/p/77cd8c0e-bc8b-434c-9456-74063b04320c?file=tracer.dc.html

Implement: tracer.dc.html
```

- 디자인에 이 프로젝트에서 구현되지 않은(=백엔드/데이터가 지원하지 않는) 기능이 포함되어 있으면 해당 UI 요소는 제거해도 됩니다. 제거한 항목은 작업 완료 후 목록으로 정리해 보고하세요.
- `tracer.dc.html`은 모바일, `tracer-web.dc.html`은 웹(데스크톱) 화면입니다.

## Worktree 작업 규칙 (중요)

**새로운 세션으로 작업을 시작할 때는 항상 새로운 worktree를 생성해서 작업하세요.**

- 세션 시작 시 `EnterWorktree`로 새 worktree를 만든 뒤 그 안에서 작업합니다.
- `main` 등 기존 체크아웃에서 직접 작업하지 않습니다.
- worktree 이름은 작업 내용을 알 수 있게 짓습니다 (예: `feat-bookmark-design`).
- 새 worktree에는 `node_modules`가 없으므로 빌드/실행 전 `npm install`이 필요할 수 있습니다.

## 기술 스택

- **빌드/런타임**: Vite 7, React 19, TypeScript 5.9 (ESM, `"type": "module"`)
- **라우팅**: react-router-dom v7
- **서버 상태**: @tanstack/react-query v5
- **클라이언트 상태**: zustand v5
- **폼/검증**: react-hook-form + zod (`@hookform/resolvers`)
- **스타일**: Tailwind CSS 3 + PostCSS
- **애니메이션**: gsap + @gsap/react (`useGSAP`)
- **아이콘**: lucide-react
- **HTTP**: axios (`@/lib/axios`)
- **결제**: @tosspayments/tosspayments-sdk

## 명령어

```bash
npm run dev      # 개발 서버 (http://localhost:3000)
npm run build    # tsc -b && vite build
npm run lint     # eslint .
npm run preview  # 빌드 미리보기
```

작업 후에는 최소한 `npx tsc -b`(타입체크)와 변경 파일에 대한 `npx eslint`를 통과시키세요.

## 경로 alias

`@/` → `frontend/src/` (vite.config.ts, tsconfig.app.json 양쪽에 설정됨). 상대경로 대신 `@/...`를 사용하세요.

## 디렉터리 구조

기능(feature) 단위로 구성합니다.

```
src/
  features/<feature>/
    api/         # react-query 훅 + axios 호출
    components/  # 해당 기능 전용 컴포넌트
    hooks/
    pages/       # 탭/화면 단위 컴포넌트
    types/
  components/ui/ # 공용 UI (TabButton, TracerTabBar, AuthGuard 등)
  layouts/       # AppLayout, AuthLayout, DesktopSidebar
  stores/        # zustand 스토어 (authStore, uiStore, folderStore ...)
  lib/           # axios, env, mock
  utils/         # cn, constants, time
  router/        # 라우트 정의
  types/         # 전역 타입
```

주요 feature: `auth`, `feed`, `saved`(북마크), `explore`, `search`, `profile`, `payment`, `notifications`.

## 디자인 시스템 (tracer)

토큰은 `tailwind.config.js`의 `theme.extend`에 정의되어 있습니다. **임의 색상 대신 정의된 토큰을 우선 사용**하세요.

### 색상 토큰
- `primary` `#17171c` (제목/강조 텍스트), `ink` `#212121` (기본 텍스트)
- `canvas` `#ffffff` (배경), `soft-stone` `#eeece7` (hover/보조 배경)
- `muted` `#93939f` (보조 텍스트/라벨), `hairline` `#d9d9dd` (테두리)
- `deep-green` `#003c33`, `action-blue` `#1863dc`, `coral` `#ff7759`/`coral-soft`
- `brand-error` `#b30000` (위험/삭제), `form-focus` `#9b60aa` (인풋 포커스)
- 자주 쓰는 임의값: 본문 보조 텍스트 `text-[#616161]`, 옅은 회색 라벨 `text-[#75758a]`, 카드 구분선 `border-[#f2f2f2]`

> 참고: `slate`(flat), `card-border`는 토큰으로 정의돼 있지 않습니다. hairline은 `border-[#f2f2f2]`, 옅은 회색 텍스트는 `text-[#75758a]`처럼 임의값을 쓰세요(기존 화면 컨벤션).

### 타이포그래피
- `font-display` (Space Grotesk): 화면 타이틀/워드마크 — 예) `font-display text-[30px] font-medium tracking-tightest text-primary`
- `font-mono` (Space Mono): eyebrow/캡션 라벨 — 예) `font-mono text-[11px] tracking-[0.6px] text-muted`, 섹션 라벨은 `tracking-mono-label`
- `font-pretendard`: 한글 본문 기본
- letter-spacing: `tracking-tightest`(-0.03em), `tracking-mono-label`(0.7px)

### 컴포넌트 컨벤션
- `cn()` (`@/utils/cn`)으로 조건부 클래스를 합칩니다.
- 아이콘은 lucide-react, `strokeWidth`는 1.6~1.8 정도의 얇은 선을 주로 사용.
- 오버레이/모달은 `AppLayout`에서 `uiStore`의 `mounted/open` 플래그 + GSAP 슬라이드로 마운트/언마운트합니다(`isXxxMounted && <XxxOverlay />`, `openXxx/closeXxx/unmountXxx` 패턴).
- 무한 스크롤은 `useIntersectionObserver`(`@/hooks`) + react-query `useInfiniteQuery`.
- 화면은 모바일 우선(`max-w-[480px]` 컨테이너), 데스크톱은 `md:`/`lg:` 분기.

## 커밋

- 커밋 메시지는 한국어, 컨벤션 prefix 사용 (`feat:`, `refactor:`, `fix:` 등). 기존 히스토리 참고.
- 사용자가 요청할 때만 커밋/푸시합니다.
