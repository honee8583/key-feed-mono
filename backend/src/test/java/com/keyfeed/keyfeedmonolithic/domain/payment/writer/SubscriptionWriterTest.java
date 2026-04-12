package com.keyfeed.keyfeedmonolithic.domain.payment.writer;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.ActiveSubscriptionAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionWriterTest {

    @InjectMocks
    private SubscriptionWriter subscriptionWriter;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    // ===== savePending =====

    @Test
    @DisplayName("savePending - PENDING 상태 구독이 저장된다")
    void savePending_PENDING_상태_저장() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        given(subscriptionRepository.save(captor.capture())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.savePending(user, paymentMethod);

        // then
        Subscription saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("savePending - 저장된 구독이 반환된다")
    void savePending_저장된_구독_반환() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        Subscription result = subscriptionWriter.savePending(user, paymentMethod);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
    }

    @Test
    @DisplayName("savePending - 유니크 인덱스 위반 시 ActiveSubscriptionAlreadyExistsException 발생")
    void savePending_유니크_인덱스_위반_커스텀예외() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        given(subscriptionRepository.save(any())).willThrow(new DataIntegrityViolationException("unique constraint"));

        // when & then
        assertThatThrownBy(() -> subscriptionWriter.savePending(user, paymentMethod))
                .isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    }

    @Test
    @DisplayName("savePending - DataIntegrityViolationException 외 예외는 그대로 전파된다")
    void savePending_기타예외_그대로전파() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        given(subscriptionRepository.save(any())).willThrow(new RuntimeException("DB 연결 오류"));

        // when & then
        assertThatThrownBy(() -> subscriptionWriter.savePending(user, paymentMethod))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    }

    // ===== updateActive =====

    @Test
    @DisplayName("updateActive - 구독 상태가 ACTIVE로 전환되고 날짜가 설정된다")
    void updateActive_ACTIVE_전환() {
        // given
        User user = makeUser(1L);
        Subscription subscription = makeSubscription(user, SubscriptionStatus.PENDING);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.updateActive(subscription);

        // then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartedAt()).isNotNull();
        assertThat(subscription.getExpiredAt()).isNotNull();
        assertThat(subscription.getNextBillingAt()).isNotNull();
    }

    @Test
    @DisplayName("updateActive - repository save가 호출된다")
    void updateActive_save_호출() {
        // given
        User user = makeUser(1L);
        Subscription subscription = makeSubscription(user, SubscriptionStatus.PENDING);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.updateActive(subscription);

        // then
        then(subscriptionRepository).should().save(subscription);
    }

    // ===== updateCanceled =====

    @Test
    @DisplayName("updateCanceled - 구독 상태가 CANCELED로 전환된다")
    void updateCanceled_CANCELED_전환() {
        // given
        User user = makeUser(1L);
        Subscription subscription = makeSubscription(user, SubscriptionStatus.PENDING);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.updateCanceled(subscription);

        // then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("updateCanceled - repository save가 호출된다")
    void updateCanceled_save_호출() {
        // given
        User user = makeUser(1L);
        Subscription subscription = makeSubscription(user, SubscriptionStatus.ACTIVE);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.updateCanceled(subscription);

        // then
        then(subscriptionRepository).should().save(subscription);
    }

    // ===== updateResume =====

    @Test
    @DisplayName("updateResume - 구독 상태가 ACTIVE로 전환되고 nextBillingAt이 갱신된다")
    void updateResume_ACTIVE_전환() {
        // given
        User user = makeUser(1L);
        PaymentMethod newPaymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, SubscriptionStatus.PAUSED);
        LocalDateTime nextBillingAt = LocalDateTime.now().plusMonths(1);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.updateResume(subscription, nextBillingAt, newPaymentMethod);

        // then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getNextBillingAt()).isEqualTo(nextBillingAt);
        assertThat(subscription.getRetryCount()).isZero();
        assertThat(subscription.getPaymentMethod()).isEqualTo(newPaymentMethod);
    }

    @Test
    @DisplayName("updateResume - repository save가 호출된다")
    void updateResume_save_호출() {
        // given
        User user = makeUser(1L);
        PaymentMethod paymentMethod = makePaymentMethod(user);
        Subscription subscription = makeSubscription(user, SubscriptionStatus.PAUSED);
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        subscriptionWriter.updateResume(subscription, LocalDateTime.now().plusMonths(1), paymentMethod);

        // then
        then(subscriptionRepository).should().save(subscription);
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

    private Subscription makeSubscription(User user, SubscriptionStatus status) {
        return Subscription.builder()
                .id(1L).user(user)
                .paymentMethod(makePaymentMethod(user))
                .status(status)
                .price(9900).orderName("프리미엄 구독 1개월")
                .startedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .retryCount(0).build();
    }
}
