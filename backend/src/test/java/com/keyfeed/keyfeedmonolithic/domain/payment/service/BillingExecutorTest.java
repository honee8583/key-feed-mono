package com.keyfeed.keyfeedmonolithic.domain.payment.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.ChargeResult;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentFailedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.writer.PaymentHistoryWriter;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BillingExecutorTest {

    @InjectMocks
    private BillingExecutor billingExecutor;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private PaymentHistoryWriter paymentHistoryWriter;

    @Test
    @DisplayName("결제 성공 - READY 선저장 후 Toss API 호출 순서 보장")
    void 결제성공_READY_선저장_순서_보장() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);
        PaymentHistory readyHistory = makeHistory(PaymentHistoryStatus.READY);
        TossBillingChargeResponse chargeResponse = makeChargeResponse();

        given(paymentHistoryWriter.saveReady(any(), any(), any(), anyString(), anyString(), anyInt()))
                .willReturn(readyHistory);
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willReturn(chargeResponse);

        // when
        billingExecutor.execute(user, paymentMethod, subscription, "프리미엄 구독 1개월", 9900);

        // then: READY 저장이 반드시 Toss API 호출 전에 발생
        InOrder inOrder = inOrder(paymentHistoryWriter, tossPaymentsClient);
        inOrder.verify(paymentHistoryWriter).saveReady(any(), any(), any(), anyString(), anyString(), anyInt());
        inOrder.verify(tossPaymentsClient).chargeBilling(anyString(), any());
    }

    @Test
    @DisplayName("결제 성공 - ChargeResult 반환, DONE 업데이트 호출")
    void 결제성공_DONE_업데이트() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);
        PaymentHistory readyHistory = makeHistory(PaymentHistoryStatus.READY);
        TossBillingChargeResponse chargeResponse = makeChargeResponse();

        given(paymentHistoryWriter.saveReady(any(), any(), any(), anyString(), anyString(), anyInt()))
                .willReturn(readyHistory);
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willReturn(chargeResponse);

        // when
        ChargeResult result = billingExecutor.execute(user, paymentMethod, subscription, "프리미엄 구독 1개월", 9900);

        // then
        assertThat(result).isNotNull();
        assertThat(result.history()).isEqualTo(readyHistory);
        then(paymentHistoryWriter).should().updateDone(eq(readyHistory), eq("pay-key-001"), any());
    }

    @Test
    @DisplayName("결제 실패 - Toss API 예외 시 FAILED 업데이트 후 예외 재전파")
    void 결제실패_FAILED_업데이트_예외재전파() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);
        PaymentHistory readyHistory = makeHistory(PaymentHistoryStatus.READY);

        given(paymentHistoryWriter.saveReady(any(), any(), any(), anyString(), anyString(), anyInt()))
                .willReturn(readyHistory);
        given(tossPaymentsClient.chargeBilling(anyString(), any()))
                .willThrow(new PaymentFailedException());

        // when & then
        assertThatThrownBy(() ->
                billingExecutor.execute(user, paymentMethod, subscription, "프리미엄 구독 1개월", 9900))
                .isInstanceOf(PaymentFailedException.class);

        then(paymentHistoryWriter).should().updateFailed(eq(readyHistory), anyString());
        then(paymentHistoryWriter).should(never()).updateDone(any(), anyString(), any());
    }

    @Test
    @DisplayName("결제 실패 - READY 저장 실패 시 Toss API 호출 안 함")
    void 결제실패_READY_저장실패_Toss호출안함() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);

        given(paymentHistoryWriter.saveReady(any(), any(), any(), anyString(), anyString(), anyInt()))
                .willThrow(new RuntimeException("DB 저장 실패"));

        // when & then
        assertThatThrownBy(() ->
                billingExecutor.execute(user, paymentMethod, subscription, "프리미엄 구독 1개월", 9900))
                .isInstanceOf(RuntimeException.class);

        then(tossPaymentsClient).should(never()).chargeBilling(anyString(), any());
    }

    @Test
    @DisplayName("결제 실행 - 고객 정보가 Toss API 요청에 포함됨")
    void 결제실행_고객정보_포함() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);
        PaymentHistory readyHistory = makeHistory(PaymentHistoryStatus.READY);
        TossBillingChargeResponse chargeResponse = makeChargeResponse();

        given(paymentHistoryWriter.saveReady(any(), any(), any(), anyString(), anyString(), anyInt()))
                .willReturn(readyHistory);
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willReturn(chargeResponse);

        // when
        billingExecutor.execute(user, paymentMethod, subscription, "프리미엄 구독 1개월", 9900);

        // then: billingKey로 Toss API 호출
        then(tossPaymentsClient).should().chargeBilling(eq("billing-key"), any());
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

    private Subscription makeSubscription(User user, PaymentMethod paymentMethod) {
        return Subscription.builder()
                .id(1L).user(user).paymentMethod(paymentMethod)
                .status(SubscriptionStatus.ACTIVE)
                .price(9900).orderName("프리미엄 구독 1개월")
                .startedAt(LocalDateTime.now().minusMonths(1))
                .nextBillingAt(LocalDateTime.now().minusHours(1))
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .retryCount(0).build();
    }

    private PaymentHistory makeHistory(PaymentHistoryStatus status) {
        try {
            PaymentHistory history = new PaymentHistory();
            setField(history, "id", 1L);
            setField(history, "orderId", "order-001");
            setField(history, "status", status);
            setField(history, "amount", 9900);
            return history;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TossBillingChargeResponse makeChargeResponse() {
        try {
            TossBillingChargeResponse response = new TossBillingChargeResponse();
            setField(response, "paymentKey", "pay-key-001");
            setField(response, "orderId", "order-001");
            setField(response, "status", "DONE");
            setField(response, "totalAmount", 9900L);
            setField(response, "approvedAt", "2026-04-05T10:00:00+09:00");
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
