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

        LocalDateTime beforeBilling = subscription.getNextBillingAt();

        // when
        billingScheduler.executeScheduledPayments();

        // then
        assertThat(subscription.getNextBillingAt()).isEqualTo(beforeBilling.plusMonths(1));
        assertThat(subscription.getRetryCount()).isZero();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
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

        // when
        billingScheduler.executeScheduledPayments();

        // then
        assertThat(subscription.getRetryCount()).isEqualTo(2);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
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
        assertThat(subscription.getRetryCount()).isEqualTo(1);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
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
        assertThat(sub1.getRetryCount()).isEqualTo(1);
        assertThat(sub2.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
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
    }

    // ===== recoverReadyPayments =====

    @Test
    @DisplayName("READY 복구 - Toss에서 DONE이면 history DONE으로 동기화")
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
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.DONE);
        assertThat(history.getPaymentKey()).isEqualTo("recover-pay-key");
    }

    @Test
    @DisplayName("READY 복구 - Toss에서 DONE이 아니면 FAILED 처리")
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
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.FAILED);
    }

    @Test
    @DisplayName("READY 복구 - Toss API 조회 실패 시 FAILED 처리 (예외 전파 안 함)")
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
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.FAILED);
    }

    @Test
    @DisplayName("READY 복구 - 복구 대상 없으면 Toss API 호출 안 함")
    void READY_복구_대상없음() {
        // given
        given(paymentHistoryRepository.findByStatusAndCreatedAtBefore(
                eq(PaymentHistoryStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        billingScheduler.recoverReadyPayments();

        // then
        then(tossPaymentsClient).should(never()).getPaymentByOrderId(anyString());
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
        try {
            PaymentHistory history = new PaymentHistory();
            setField(history, "id", 1L);
            setField(history, "orderId", "order-ready-001");
            setField(history, "status", PaymentHistoryStatus.READY);
            setField(history, "amount", 9900);
            return history;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TossPaymentQueryResponse makeQueryResponse(String status) {
        try {
            TossPaymentQueryResponse response = new TossPaymentQueryResponse();
            setField(response, "orderId", "order-ready-001");
            setField(response, "paymentKey", "recover-pay-key");
            setField(response, "status", status);
            setField(response, "approvedAt", "DONE".equals(status) ? "2026-04-05T10:00:00+09:00" : null);
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
