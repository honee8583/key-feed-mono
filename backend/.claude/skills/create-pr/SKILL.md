---
name: create-pr
description: >
    현재 브랜치를 기준으로 PR을 생성.
---

## 규칙
- 테이블 마이그레이션 작업이 들어간 경우 본문에 표 형식으로 변경/추가된 사항을 명시.
- PR제목에는 관련 이슈 명시 X
- API를 구현한 경우 본문의 요약파트에 해당 API에서 발생하는 쿼리 수를 명시해줘

## PR 템플릿 형식

<github_pr_template>
## 제목(본문 아님)
feat: 주문 취소 API 구현

## 요약
- 주문 취소 API 추가: `POST /api/orders/{orderId}/cancel`
- `OrderCancelService` 신규 추가 (취소 검증 → 환불 → 재고 원복)
- `OrderStatus`에 `CANCELED` 상태 추가
- 토스페이먼츠 환불 연동을 위한 `PaymentRefundClient` 추가

## 변경 이유
- 취소 가능 상태 검증, 환불, 재고 원복은 하나라도 실패하면 전체
  롤백되어야 하므로 `@Transactional`로 묶어 처리함.
- 환불 API는 외부 호출이라 실패 시 재시도가 필요해 보였으나, 이번
  범위에서는 실패 시 트랜잭션 롤백 + 예외 전파로만 처리하고 재시도
  큐 도입은 후속 이슈(#130)로 분리함.

## 구현 방식
- 상태 검증: `OrderStatus.isCancelable()` 메서드로 PAID, READY만 허용
- 동시성: 동일 주문 중복 취소 방지를 위해 `SELECT ... FOR UPDATE`
  (비관적 락) 적용
- 환불 실패 시 `PaymentRefundException` 발생 → 트랜잭션 롤백

## DB 변경 (있을 경우)
- `order` 테이블 `status` enum에 `CANCELED` 추가
- 마이그레이션: `V12__add_canceled_status.sql` (Flyway)

## 리뷰 포인트(개발자들끼리 상의하고 싶은 문제가 있다면)
- 비관적 락 vs 낙관적 락 선택이 적절한지 의견 부탁드립니다.
- 환불 실패 시 롤백만으로 충분한지, 보상 트랜잭션이 필요한지

## 체크리스트
- [x] 테스트 통과
- [x] 셀프 리뷰 완료
- [x] 마이그레이션 스크립트 작성
- [ ] 운영 환경변수 등록 (배포 담당자 확인 필요)

## 관련 이슈
Closes #123
