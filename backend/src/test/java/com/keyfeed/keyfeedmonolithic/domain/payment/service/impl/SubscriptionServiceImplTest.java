package com.keyfeed.keyfeedmonolithic.domain.payment.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossPaymentCancelRequest;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentMethodRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    // ===== startSubscription =====

    @Test
    @DisplayName("구독 시작 성공 - 첫 결제 후 ACTIVE 상태의 구독이 생성된다")
    void 구독_시작_성공() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(methodId, user);
        TossBillingChargeResponse chargeResponse = makeChargeResponse();

        given(subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).willReturn(false);
        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(paymentMethod));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willReturn(chargeResponse);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        SubscriptionStartRequestDto request = makeStartRequest(methodId);
        SubscriptionStartResponseDto result = subscriptionService.startSubscription(userId, request);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getNextBillingAt()).isNotNull();
        assertThat(result.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("구독 시작 실패 - 이미 ACTIVE 구독이 존재하면 409 예외")
    void 구독_시작_실패_중복구독() {
        Long userId = 1L;
        given(subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> subscriptionService.startSubscription(userId, makeStartRequest(10L)))
                .isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    }

    @Test
    @DisplayName("구독 시작 실패 - 존재하지 않는 결제 수단이면 404 예외")
    void 구독_시작_실패_결제수단_없음() {
        Long userId = 1L;
        given(subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).willReturn(false);
        given(paymentMethodRepository.findByIdAndIsActiveTrue(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.startSubscription(userId, makeStartRequest(99L)))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("구독 시작 실패 - 타인 소유 결제 수단이면 404 예외")
    void 구독_시작_실패_결제수단_소유권_불일치() {
        Long userId = 1L;
        Long otherUserId = 2L;
        User otherUser = makeUser(otherUserId);
        PaymentMethod otherMethod = makePaymentMethod(10L, otherUser);

        given(subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).willReturn(false);
        given(paymentMethodRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(otherMethod));

        assertThatThrownBy(() -> subscriptionService.startSubscription(userId, makeStartRequest(10L)))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("구독 시작 실패 - 토스 결제 실패 시 payment_history FAILED 기록 후 예외")
    void 구독_시작_실패_결제실패() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(methodId, user);
        PaymentHistory[] savedHistory = new PaymentHistory[1];

        given(subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).willReturn(false);
        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(paymentMethod));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(paymentHistoryRepository.save(any())).willAnswer(i -> {
            savedHistory[0] = i.getArgument(0);
            return savedHistory[0];
        });
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willThrow(new PaymentFailedException());

        assertThatThrownBy(() -> subscriptionService.startSubscription(userId, makeStartRequest(methodId)))
                .isInstanceOf(PaymentFailedException.class);

        assertThat(savedHistory[0].getStatus()).isEqualTo(PaymentHistoryStatus.FAILED);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    // ===== getMySubscription =====

    @Test
    @DisplayName("구독 조회 성공 - ACTIVE 구독이 있으면 상태 반환")
    void 구독_조회_성공_ACTIVE() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.ACTIVE);

        given(subscriptionRepository.findTopByUserIdAndStatusInOrderByCreatedAtDesc(eq(userId), any()))
                .willReturn(Optional.of(subscription));

        SubscriptionStatusResponseDto result = subscriptionService.getMySubscription(userId);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getSubscriptionId()).isEqualTo(subscription.getId());
    }

    @Test
    @DisplayName("구독 조회 성공 - 구독이 없으면 status NONE 반환")
    void 구독_조회_성공_구독없음() {
        Long userId = 1L;
        given(subscriptionRepository.findTopByUserIdAndStatusInOrderByCreatedAtDesc(eq(userId), any()))
                .willReturn(Optional.empty());

        SubscriptionStatusResponseDto result = subscriptionService.getMySubscription(userId);

        assertThat(result.getStatus()).isEqualTo("NONE");
        assertThat(result.getSubscriptionId()).isNull();
    }

    @Test
    @DisplayName("구독 조회 성공 - CANCELED 구독이면 canceledAt이 포함된다")
    void 구독_조회_성공_CANCELED() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.CANCELED);
        subscription.cancel();

        given(subscriptionRepository.findTopByUserIdAndStatusInOrderByCreatedAtDesc(eq(userId), any()))
                .willReturn(Optional.of(subscription));

        SubscriptionStatusResponseDto result = subscriptionService.getMySubscription(userId);

        assertThat(result.getStatus()).isEqualTo("CANCELED");
        assertThat(result.getCanceledAt()).isNotNull();
    }

    // ===== cancelSubscription =====

    @Test
    @DisplayName("구독 해지 성공 - ACTIVE 구독이 CANCELED로 변경된다")
    void 구독_해지_성공() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.ACTIVE);

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.of(subscription));

        SubscriptionCancelResponseDto result = subscriptionService.cancelSubscription(userId);

        assertThat(result.getStatus()).isEqualTo("CANCELED");
        assertThat(result.getCanceledAt()).isNotNull();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    @DisplayName("구독 해지 실패 - ACTIVE 구독이 없으면 404 예외")
    void 구독_해지_실패_활성구독없음() {
        Long userId = 1L;
        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    // ===== resumeSubscription =====

    @Test
    @DisplayName("구독 재개 성공 - PAUSED 구독이 ACTIVE로 복구되고 retryCount가 0이 된다")
    void 구독_재개_성공() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(methodId, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.PAUSED);
        TossBillingChargeResponse chargeResponse = makeChargeResponse();

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PAUSED))
                .willReturn(Optional.of(subscription));
        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(paymentMethod));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willReturn(chargeResponse);
        given(paymentHistoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        SubscriptionResumeRequestDto request = makeResumeRequest(methodId);
        SubscriptionResumeResponseDto result = subscriptionService.resumeSubscription(userId, request);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getNextBillingAt()).isNotNull();
        assertThat(subscription.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("구독 재개 실패 - PAUSED 구독이 없으면 404 예외")
    void 구독_재개_실패_일시정지구독없음() {
        Long userId = 1L;
        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PAUSED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.resumeSubscription(userId, makeResumeRequest(10L)))
                .isInstanceOf(PausedSubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("구독 재개 실패 - 유효하지 않은 결제 수단이면 404 예외")
    void 구독_재개_실패_결제수단없음() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.PAUSED);

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PAUSED))
                .willReturn(Optional.of(subscription));
        given(paymentMethodRepository.findByIdAndIsActiveTrue(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.resumeSubscription(userId, makeResumeRequest(99L)))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("구독 재개 실패 - 결제 실패 시 PAUSED 상태 유지 및 payment_history FAILED 기록")
    void 구독_재개_실패_결제실패_상태유지() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(methodId, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.PAUSED);
        PaymentHistory[] savedHistory = new PaymentHistory[1];

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PAUSED))
                .willReturn(Optional.of(subscription));
        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(paymentMethod));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(paymentHistoryRepository.save(any())).willAnswer(i -> {
            savedHistory[0] = i.getArgument(0);
            return savedHistory[0];
        });
        given(tossPaymentsClient.chargeBilling(anyString(), any())).willThrow(new PaymentFailedException());

        assertThatThrownBy(() -> subscriptionService.resumeSubscription(userId, makeResumeRequest(methodId)))
                .isInstanceOf(PaymentFailedException.class);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(savedHistory[0].getStatus()).isEqualTo(PaymentHistoryStatus.FAILED);
    }

    // ===== refundSubscription =====

    @Test
    @DisplayName("구독 취소(환불) 성공 - 결제일 1일 이내 취소 시 REFUNDED 상태로 전환된다")
    void 구독_취소_성공() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.ACTIVE);
        PaymentHistory history = makeHistory(subscription);

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.of(subscription));
        given(paymentHistoryRepository.findTopBySubscriptionIdAndStatusOrderByCreatedAtDesc(
                subscription.getId(), PaymentHistoryStatus.DONE))
                .willReturn(Optional.of(history));
        willDoNothing().given(tossPaymentsClient).cancelPayment(anyString(), any());

        SubscriptionRefundResponseDto result = subscriptionService.refundSubscription(userId);

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        assertThat(result.getCanceledAt()).isNotNull();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.REFUNDED);
        assertThat(history.getStatus()).isEqualTo(PaymentHistoryStatus.CANCELED);
    }

    @Test
    @DisplayName("구독 취소(환불) 실패 - ACTIVE 구독 없으면 404 예외")
    void 구독_취소_실패_활성구독없음() {
        Long userId = 1L;
        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.refundSubscription(userId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("구독 취소(환불) 실패 - 결제일로부터 1일 초과 시 422 예외")
    void 구독_취소_실패_환불기간초과() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = Subscription.builder()
                .id(1L)
                .user(user)
                .paymentMethod(paymentMethod)
                .status(SubscriptionStatus.ACTIVE)
                .price(9900)
                .orderName("프리미엄 구독 1개월")
                .startedAt(LocalDateTime.now().minusDays(2))  // 2일 전 결제
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .retryCount(0)
                .build();

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.refundSubscription(userId))
                .isInstanceOf(RefundPeriodExpiredException.class);
    }

    @Test
    @DisplayName("구독 취소(환불) 실패 - 토스 환불 API 실패 시 RefundFailedException")
    void 구독_취소_실패_토스API실패() {
        Long userId = 1L;
        User user = makeUser(userId);
        PaymentMethod paymentMethod = makePaymentMethod(10L, user);
        Subscription subscription = makeSubscription(user, paymentMethod, SubscriptionStatus.ACTIVE);
        PaymentHistory history = makeHistory(subscription);

        given(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.of(subscription));
        given(paymentHistoryRepository.findTopBySubscriptionIdAndStatusOrderByCreatedAtDesc(
                subscription.getId(), PaymentHistoryStatus.DONE))
                .willReturn(Optional.of(history));
        willThrow(new RuntimeException("Toss API error")).given(tossPaymentsClient).cancelPayment(anyString(), any());

        assertThatThrownBy(() -> subscriptionService.refundSubscription(userId))
                .isInstanceOf(RefundFailedException.class);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
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

    private PaymentMethod makePaymentMethod(Long id, User user) {
        return PaymentMethod.builder()
                .id(id)
                .user(user)
                .billingKey("billing-key-test")
                .methodType(PaymentMethodType.CARD)
                .providerName("신한")
                .displayNumber("4330****0000")
                .isDefault(true)
                .isActive(true)
                .build();
    }

    private Subscription makeSubscription(User user, PaymentMethod paymentMethod, SubscriptionStatus status) {
        return Subscription.builder()
                .id(1L)
                .user(user)
                .paymentMethod(paymentMethod)
                .status(status)
                .price(9900)
                .orderName("프리미엄 구독 1개월")
                .startedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .retryCount(0)
                .build();
    }

    private TossBillingChargeResponse makeChargeResponse() {
        try {
            TossBillingChargeResponse response = new TossBillingChargeResponse();
            setField(response, "paymentKey", "payment-key-test");
            setField(response, "orderId", "order-id-test");
            setField(response, "status", "DONE");
            setField(response, "totalAmount", 9900L);
            setField(response, "approvedAt", "2024-01-15T10:30:00+09:00");
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PaymentHistory makeHistory(Subscription subscription) {
        try {
            PaymentHistory history = new PaymentHistory();
            setField(history, "id", 1L);
            setField(history, "subscription", subscription);
            setField(history, "paymentKey", "payment-key-test");
            setField(history, "status", PaymentHistoryStatus.DONE);
            setField(history, "amount", 9900);
            setField(history, "orderId", "order-id-test");
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

    private SubscriptionStartRequestDto makeStartRequest(Long methodId) {
        try {
            var dto = new SubscriptionStartRequestDto();
            var field = SubscriptionStartRequestDto.class.getDeclaredField("methodId");
            field.setAccessible(true);
            field.set(dto, methodId);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SubscriptionResumeRequestDto makeResumeRequest(Long methodId) {
        try {
            var dto = new SubscriptionResumeRequestDto();
            var field = SubscriptionResumeRequestDto.class.getDeclaredField("methodId");
            field.setAccessible(true);
            field.set(dto, methodId);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
