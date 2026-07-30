package com.keyfeed.keyfeedmonolithic.domain.payment.scheduler;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.notification.service.NotificationService;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.ChargeResult;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentFailedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.BillingExecutor;
import com.keyfeed.keyfeedmonolithic.domain.payment.writer.PaymentHistoryWriter;
import com.keyfeed.keyfeedmonolithic.domain.payment.writer.SubscriptionWriter;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossPaymentQueryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

    @InjectMocks
    private BillingScheduler billingScheduler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BillingExecutor billingExecutor;

    @Mock
    private SubscriptionWriter subscriptionWriter;

    @Mock
    private PaymentHistoryWriter paymentHistoryWriter;

    // ===== executeScheduledPayments =====

    @Test
    @DisplayName("자동 결제 성공 - nextBillingAt +1달 갱신, retryCount 초기화")
    void 자동결제_성공() {
        // given
        Subscription subscription = makeActiveSubscription(0);
        ChargeResult chargeResult = makeChargeResult(subscription);

        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(subscription));
        given(billingExecutor.execute(any(), any(), eq(subscription), anyString(), anyInt()))
                .willReturn(chargeResult);

        // when
        billingScheduler.executeScheduledPayments();

        // then
        then(subscriptionWriter).should().updateBillingSuccess(subscription);
        then(subscriptionWriter).should(never()).updateBillingFailure(any(), anyInt());
        then(notificationService).should(never()).send(any());
    }

    @Test
    @DisplayName("자동 결제 실패 - retryCount 1 증가, ACTIVE 유지 (3회 미만)")
    void 자동결제_실패_retryCount_증가() {
        // given
        Subscription subscription = makeActiveSubscription(1);  // 기존 retryCount=1

        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(subscription));
        given(billingExecutor.execute(any(), any(), eq(subscription), anyString(), anyInt()))
                .willThrow(new PaymentFailedException());
        willAnswer(invocation -> {
            subscription.increaseRetryCount();
            return null;
        }).given(subscriptionWriter).updateBillingFailure(subscription, 3);

        // when
        billingScheduler.executeScheduledPayments();

        // then
        assertThat(subscription.getRetryCount()).isEqualTo(2);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        then(subscriptionWriter).should().updateBillingFailure(subscription, 3);
        then(notificationService).should(never()).send(any());
    }

    @Test
    @DisplayName("자동 결제 실패 3회 - PAUSED 전환 및 알림 발송")
    void 자동결제_실패_3회_PAUSED_전환() {
        // given
        Subscription subscription = makeActiveSubscription(2);  // 기존 retryCount=2

        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(subscription));
        given(billingExecutor.execute(any(), any(), eq(subscription), anyString(), anyInt()))
                .willThrow(new PaymentFailedException());
        willAnswer(invocation -> {
            subscription.increaseRetryCount();
            subscription.pause();
            return null;
        }).given(subscriptionWriter).updateBillingFailure(subscription, 3);

        // when
        billingScheduler.executeScheduledPayments();

        // then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getRetryCount()).isEqualTo(3);
        then(notificationService).should().send(argThat(dto ->
                dto.getUserId().equals(subscription.getUser().getId())
        ));
    }

    @Test
    @DisplayName("자동 결제 실패 - InvalidPaymentMethodException도 retryCount 증가")
    void 자동결제_실패_InvalidPaymentMethodException() {
        // given
        Subscription subscription = makeActiveSubscription(0);

        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(subscription));
        given(billingExecutor.execute(any(), any(), eq(subscription), anyString(), anyInt()))
                .willThrow(new InvalidPaymentMethodException());

        // when
        billingScheduler.executeScheduledPayments();

        // then
        then(subscriptionWriter).should().updateBillingFailure(subscription, 3);
        then(subscriptionWriter).should(never()).updateBillingSuccess(any());
    }

    @Test
    @DisplayName("결제 대상 없음 - BillingExecutor 호출 안 함")
    void 자동결제_대상없음() {
        // given
        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        billingScheduler.executeScheduledPayments();

        // then
        then(billingExecutor).should(never()).execute(any(), any(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("다건 처리 - 한 건 실패해도 나머지 건 계속 처리")
    void 자동결제_다건_한건실패_나머지처리() {
        // given
        Subscription sub1 = makeActiveSubscription(0);
        Subscription sub2 = makeActiveSubscriptionWithId(2L, 0);
        ChargeResult chargeResult = makeChargeResult(sub2);

        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(sub1, sub2));
        given(billingExecutor.execute(any(), any(), eq(sub1), anyString(), anyInt()))
                .willThrow(new PaymentFailedException());
        given(billingExecutor.execute(any(), any(), eq(sub2), anyString(), anyInt()))
                .willReturn(chargeResult);

        // when
        billingScheduler.executeScheduledPayments();

        // then
        then(subscriptionWriter).should().updateBillingFailure(sub1, 3);
        then(subscriptionWriter).should().updateBillingSuccess(sub2);
        then(billingExecutor).should(times(2)).execute(any(), any(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("인프라 오류 - retryCount 증가 없이 로그만 기록")
    void 자동결제_인프라오류_retryCount_증가없음() {
        // given
        Subscription subscription = makeActiveSubscription(0);

        given(subscriptionRepository.findByStatusAndNextBillingAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(subscription));
        given(billingExecutor.execute(any(), any(), eq(subscription), anyString(), anyInt()))
                .willThrow(new RuntimeException("네트워크 오류"));

        // when & then
        assertThatCode(() -> billingScheduler.executeScheduledPayments()).doesNotThrowAnyException();
        assertThat(subscription.getRetryCount()).isZero();
        then(subscriptionWriter).shouldHaveNoInteractions();
    }

    // ===== recoverReadyPayments =====

    @Test
    @DisplayName("READY 복구 - Toss에서 DONE이면 updateDone으로 동기화 (paymentKey, approvedAt 전달)")
    void READY_복구_DONE_동기화() {
        // given
        PaymentHistory history = makeReadyHistory();
        TossPaymentQueryResponse queryResponse = makeQueryResponse("DONE");

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history));
        given(tossPaymentsClient.getPaymentByOrderId(history.getOrderId())).willReturn(queryResponse);

        // when
        billingScheduler.recoverReadyPayments();

        // then
        then(paymentHistoryWriter).should().updateDone(
                history, "recover-pay-key", LocalDateTime.of(2026, 4, 5, 10, 0));
        then(paymentHistoryWriter).should(never()).updateFailed(any(), anyString());
    }

    @Test
    @DisplayName("READY 복구 - approvedAt이 null이면 updateDone에 null 전달")
    void READY_복구_DONE_approvedAt_null() {
        // given
        PaymentHistory history = makeReadyHistory();
        TossPaymentQueryResponse queryResponse = makeQueryResponse("DONE", null);

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history));
        given(tossPaymentsClient.getPaymentByOrderId(history.getOrderId())).willReturn(queryResponse);

        // when
        billingScheduler.recoverReadyPayments();

        // then
        then(paymentHistoryWriter).should().updateDone(eq(history), eq("recover-pay-key"), isNull());
    }

    @Test
    @DisplayName("READY 복구 - approvedAt 파싱 실패 시 updateDone에 null 전달 (예외 전파 안 함)")
    void READY_복구_DONE_approvedAt_파싱실패() {
        // given
        PaymentHistory history = makeReadyHistory();
        TossPaymentQueryResponse queryResponse = makeQueryResponse("DONE", "잘못된-날짜-형식");

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history));
        given(tossPaymentsClient.getPaymentByOrderId(history.getOrderId())).willReturn(queryResponse);

        // when & then
        assertThatCode(() -> billingScheduler.recoverReadyPayments()).doesNotThrowAnyException();
        then(paymentHistoryWriter).should().updateDone(eq(history), eq("recover-pay-key"), isNull());
    }

    @Test
    @DisplayName("READY 복구 - Toss에서 DONE이 아니면 updateFailed 처리")
    void READY_복구_미완료_FAILED처리() {
        // given
        PaymentHistory history = makeReadyHistory();
        TossPaymentQueryResponse queryResponse = makeQueryResponse("ABORTED");

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history));
        given(tossPaymentsClient.getPaymentByOrderId(history.getOrderId())).willReturn(queryResponse);

        // when
        billingScheduler.recoverReadyPayments();

        // then
        then(paymentHistoryWriter).should().updateFailed(history, "결제 미완료 상태로 방치된 결제 건");
        then(paymentHistoryWriter).should(never()).updateDone(any(), anyString(), any());
    }

    @Test
    @DisplayName("READY 복구 - Toss API 조회 실패 시 updateFailed 처리 (예외 전파 안 함)")
    void READY_복구_Toss조회실패_FAILED처리() {
        // given
        PaymentHistory history = makeReadyHistory();

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history));
        given(tossPaymentsClient.getPaymentByOrderId(anyString()))
                .willThrow(new RuntimeException("Toss API 오류"));

        // when & then
        assertThatCode(() -> billingScheduler.recoverReadyPayments()).doesNotThrowAnyException();
        then(paymentHistoryWriter).should().updateFailed(history, "복구 중 오류 발생: Toss API 오류");
        then(paymentHistoryWriter).should(never()).updateDone(any(), anyString(), any());
    }

    @Test
    @DisplayName("READY 복구 - 복구 대상 없으면 Toss API/Writer 호출 안 함")
    void READY_복구_대상없음() {
        // given
        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        billingScheduler.recoverReadyPayments();

        // then
        then(tossPaymentsClient).should(never()).getPaymentByOrderId(anyString());
        then(paymentHistoryWriter).should(never()).updateDone(any(), anyString(), any());
        then(paymentHistoryWriter).should(never()).updateFailed(any(), anyString());
    }

    @Test
    @DisplayName("READY 복구 다건 - 첫 건 Toss 조회 실패해도 나머지 건 계속 처리")
    void READY_복구_다건_첫건_Toss조회실패_나머지처리() {
        // given
        PaymentHistory history1 = makeReadyHistory(1L, "order-ready-001");
        PaymentHistory history2 = makeReadyHistory(2L, "order-ready-002");
        TossPaymentQueryResponse doneResponse = makeQueryResponse("DONE");

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history1, history2));
        given(tossPaymentsClient.getPaymentByOrderId("order-ready-001"))
                .willThrow(new RuntimeException("Toss API 오류"));
        given(tossPaymentsClient.getPaymentByOrderId("order-ready-002")).willReturn(doneResponse);

        // when
        billingScheduler.recoverReadyPayments();

        // then
        then(tossPaymentsClient).should(times(2)).getPaymentByOrderId(anyString());
        then(paymentHistoryWriter).should().updateFailed(history1, "복구 중 오류 발생: Toss API 오류");
        then(paymentHistoryWriter).should().updateDone(
                history2, "recover-pay-key", LocalDateTime.of(2026, 4, 5, 10, 0));
    }

    @Test
    @DisplayName("READY 복구 다건 - 첫 건 updateDone 예외 시 updateFailed로 마킹하고 나머지 건 계속 처리")
    void READY_복구_다건_첫건_updateDone예외_나머지처리() {
        // given
        PaymentHistory history1 = makeReadyHistory(1L, "order-ready-001");
        PaymentHistory history2 = makeReadyHistory(2L, "order-ready-002");
        TossPaymentQueryResponse doneResponse = makeQueryResponse("DONE");

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history1, history2));
        given(tossPaymentsClient.getPaymentByOrderId("order-ready-001")).willReturn(doneResponse);
        given(tossPaymentsClient.getPaymentByOrderId("order-ready-002")).willReturn(doneResponse);
        willThrow(new RuntimeException("DB 오류"))
                .given(paymentHistoryWriter).updateDone(eq(history1), anyString(), any());

        // when & then
        assertThatCode(() -> billingScheduler.recoverReadyPayments()).doesNotThrowAnyException();
        then(paymentHistoryWriter).should().updateFailed(history1, "복구 중 오류 발생: DB 오류");
        then(paymentHistoryWriter).should().updateDone(
                history2, "recover-pay-key", LocalDateTime.of(2026, 4, 5, 10, 0));
    }

    @Test
    @DisplayName("READY 복구 다건 - updateFailed 자체가 예외를 던져도 예외 없이 나머지 건 계속 처리")
    void READY_복구_다건_updateFailed예외_전파안함() {
        // given
        PaymentHistory history1 = makeReadyHistory(1L, "order-ready-001");
        PaymentHistory history2 = makeReadyHistory(2L, "order-ready-002");
        TossPaymentQueryResponse abortedResponse = makeQueryResponse("ABORTED");
        TossPaymentQueryResponse doneResponse = makeQueryResponse("DONE");

        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(history1, history2));
        given(tossPaymentsClient.getPaymentByOrderId("order-ready-001")).willReturn(abortedResponse);
        given(tossPaymentsClient.getPaymentByOrderId("order-ready-002")).willReturn(doneResponse);
        willThrow(new RuntimeException("DB 오류"))
                .given(paymentHistoryWriter).updateFailed(eq(history1), anyString());

        // when & then
        assertThatCode(() -> billingScheduler.recoverReadyPayments()).doesNotThrowAnyException();
        then(paymentHistoryWriter).should(times(2)).updateFailed(eq(history1), anyString());
        then(paymentHistoryWriter).should().updateDone(
                history2, "recover-pay-key", LocalDateTime.of(2026, 4, 5, 10, 0));
    }

    // ===== helpers =====

    private User makeUser(Long id) {
        return User.builder()
                .id(id)
                .email("test@test.com")
                .username("테스터")
                .customerKey("customer-key-" + id)
                .build();
    }

    private PaymentMethod makePaymentMethod(User user) {
        return PaymentMethod.builder()
                .id(1L).user(user).billingKey("billing-key")
                .methodType(PaymentMethodType.CARD)
                .providerName("신한").displayNumber("4330****0000")
                .isDefault(true).isActive(true).build();
    }

    private Subscription makeActiveSubscription(int retryCount) {
        return makeActiveSubscriptionWithId(1L, retryCount);
    }

    private Subscription makeActiveSubscriptionWithId(Long id, int retryCount) {
        User user = makeUser(id);
        return Subscription.builder()
                .id(id).user(user).paymentMethod(makePaymentMethod(user))
                .status(SubscriptionStatus.ACTIVE)
                .price(9900).orderName("프리미엄 구독 1개월")
                .startedAt(LocalDateTime.now().minusMonths(1))
                .nextBillingAt(LocalDateTime.now().minusHours(1))
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .retryCount(retryCount).build();
    }

    private ChargeResult makeChargeResult(Subscription subscription) {
        try {
            PaymentHistory history = new PaymentHistory();
            setField(history, "id", 1L);
            setField(history, "orderId", "order-001");
            setField(history, "status", PaymentHistoryStatus.DONE);
            setField(history, "paymentKey", "pay-key-001");
            setField(history, "amount", 9900);

            var response = new com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse();
            setField(response, "paymentKey", "pay-key-001");
            setField(response, "orderId", "order-001");
            setField(response, "status", "DONE");
            setField(response, "totalAmount", 9900L);
            setField(response, "approvedAt", "2026-04-05T10:00:00+09:00");

            return new ChargeResult(history, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PaymentHistory makeReadyHistory() {
        return makeReadyHistory(1L, "order-ready-001");
    }

    private PaymentHistory makeReadyHistory(Long id, String orderId) {
        try {
            PaymentHistory history = new PaymentHistory();
            setField(history, "id", id);
            setField(history, "orderId", orderId);
            setField(history, "status", PaymentHistoryStatus.READY);
            setField(history, "amount", 9900);
            return history;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TossPaymentQueryResponse makeQueryResponse(String status) {
        return makeQueryResponse(status, "DONE".equals(status) ? "2026-04-05T10:00:00+09:00" : null);
    }

    private TossPaymentQueryResponse makeQueryResponse(String status, String approvedAt) {
        try {
            TossPaymentQueryResponse response = new TossPaymentQueryResponse();
            setField(response, "orderId", "order-ready-001");
            setField(response, "paymentKey", "recover-pay-key");
            setField(response, "status", status);
            setField(response, "approvedAt", approvedAt);
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
