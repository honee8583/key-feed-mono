package com.keyfeed.keyfeedmonolithic.domain.payment.writer;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryWriterTest {

    @InjectMocks
    private PaymentHistoryWriter paymentHistoryWriter;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    // ===== saveReady =====

    @Test
    @DisplayName("saveReady - READY 상태로 결제 내역이 저장된다")
    void saveReady_READY_상태_저장() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        given(paymentHistoryRepository.save(captor.capture())).willAnswer(i -> i.getArgument(0));

        // when
        paymentHistoryWriter.saveReady(user, paymentMethod, subscription, "order-001", "프리미엄 구독 1개월", 9900);

        // then
        PaymentHistory saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentHistoryStatus.READY);
        assertThat(saved.getOrderId()).isEqualTo("order-001");
        assertThat(saved.getOrderName()).isEqualTo("프리미엄 구독 1개월");
        assertThat(saved.getAmount()).isEqualTo(9900);
        assertThat(saved.getMethodType()).isEqualTo(PaymentMethodType.CARD);
    }

    @Test
    @DisplayName("saveReady - user, paymentMethod, subscription이 올바르게 연결된다")
    void saveReady_연관관계_설정() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, paymentMethod);
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        given(paymentHistoryRepository.save(captor.capture())).willAnswer(i -> i.getArgument(0));

        // when
        paymentHistoryWriter.saveReady(user, paymentMethod, subscription, "order-001", "프리미엄 구독 1개월", 9900);

        // then
        PaymentHistory saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(saved.getSubscription()).isEqualTo(subscription);
    }

    @Test
    @DisplayName("saveReady - 저장된 결제 내역이 반환된다")
    void saveReady_저장된_내역_반환() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        PaymentHistory result = paymentHistoryWriter.saveReady(
                user, paymentMethod, null, "order-001", "프리미엄 구독 1개월", 9900);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentHistoryStatus.READY);
    }

    // ===== updateDone =====

    @Test
    @DisplayName("updateDone - 상태가 DONE으로 변경되고 paymentKey가 저장된다")
    void updateDone_DONE_전환() {
        // given
        PaymentHistory history = makeHistory(PaymentHistoryStatus.READY);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        paymentHistoryWriter.updateDone(history, "pay-key-001", LocalDateTime.now());

        // then
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.DONE);
        assertThat(history.getPaymentKey()).isEqualTo("pay-key-001");
    }

    @Test
    @DisplayName("updateDone - approvedAt이 null이어도 저장된다")
    void updateDone_approvedAt_null_허용() {
        // given
        PaymentHistory history = makeHistory(PaymentHistoryStatus.READY);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when & then
        assertThatCode(() -> paymentHistoryWriter.updateDone(history, "pay-key-001", null))
                .doesNotThrowAnyException();
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.DONE);
    }

    @Test
    @DisplayName("updateDone - repository save가 호출된다")
    void updateDone_save_호출() {
        // given
        PaymentHistory history = makeHistory(PaymentHistoryStatus.READY);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        paymentHistoryWriter.updateDone(history, "pay-key-001", LocalDateTime.now());

        // then
        then(paymentHistoryRepository).should().save(history);
    }

    // ===== updateFailed =====

    @Test
    @DisplayName("updateFailed - 상태가 FAILED로 변경되고 실패 사유가 저장된다")
    void updateFailed_FAILED_전환() {
        // given
        PaymentHistory history = makeHistory(PaymentHistoryStatus.READY);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        paymentHistoryWriter.updateFailed(history, "카드 한도 초과");

        // then
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.FAILED);
        assertThat(history.getFailReason()).isEqualTo("카드 한도 초과");
    }

    @Test
    @DisplayName("updateFailed - repository save가 호출된다")
    void updateFailed_save_호출() {
        // given
        PaymentHistory history = makeHistory(PaymentHistoryStatus.READY);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        paymentHistoryWriter.updateFailed(history, "오류");

        // then
        then(paymentHistoryRepository).should().save(history);
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
                .startedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
