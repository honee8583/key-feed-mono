---
description: 특정 PR 리뷰를 가져와 코드 수정
args: [PR_NUMBER]
allowed-tools: ["Bash(gh*)"]
---
## PR 리뷰 수정 도우미

다음 PR의 리뷰를 분석해서 코드를 수정하세요:

1. PR 정보 확인: `gh pr view $ARGUMENTS --json title,url,reviews`
2. 상세 리뷰: `gh pr view $ARGUMENTS --json reviews --jq '.reviews[] | "\(.author.login): \(.body)"'`
3. 수정 대상 파일 찾기: `gh pr diff $ARGUMENTS`
4. 리뷰 코멘트 기반으로 코드 개선

**리뷰 우선순위:**
- 버그 수정 요청
- 성능 개선 제안
- 코드 스타일/컨벤션
- 아키텍처 변경 요청

수정 후:
- 변경사항 요약
- `git commit` 메시지 제안
- 필요시 추가 테스트 실행
