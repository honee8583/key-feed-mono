package com.keyfeed.keyfeedmonolithic.domain.payment.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentMethodRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.BillingExecutor;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.SubscriptionService;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossPaymentCancelRequest;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final int SUBSCRIPTION_PRICE = 100;
    private static final String SUBSCRIPTION_ORDER_NAME = "프리미엄 구독 1개월";

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final BillingExecutor billingExecutor;

    @Override
    @Transactional
    public SubscriptionStartResponseDto startSubscription(Long userId, SubscriptionStartRequestDto request) {
        // 1. 사용자 락 조회 (토스 API 호출에 필요한 customerKey, email, name)
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // 2. 이미 ACTIVE인 구독이 있는지 검증 (중복 구독 방지)
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new ActiveSubscriptionAlreadyExistsException();
        }

        // 3. methodId가 본인 소유이고 활성 상태인지 검증
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndIsActiveTrue(request.getMethodId())
                .orElseThrow(PaymentMethodNotFoundException::new);

        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new PaymentMethodNotFoundException();
        }

        // 4. 결제 실행 (READY 선저장 → chargeBilling → markDone/markFailed)
        ChargeResult result = billingExecutor.execute(user, paymentMethod, null, SUBSCRIPTION_ORDER_NAME, SUBSCRIPTION_PRICE);

        // 5. subscription INSERT (status: ACTIVE, 만료일/다음 결제일: 현재 +1달)
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .paymentMethod(paymentMethod)
                .status(SubscriptionStatus.ACTIVE)
                .price(SUBSCRIPTION_PRICE)
                .orderName(SUBSCRIPTION_ORDER_NAME)
                .startedAt(now)
                .expiredAt(now.plusMonths(1))
                .nextBillingAt(now.plusMonths(1))
                .retryCount(0)
                .build();
        subscriptionRepository.save(subscription);

        // 6. payment_history에 생성된 subscription 연결
        result.history().linkSubscription(subscription);

        return SubscriptionStartResponseDto.from(subscription, result.history());
    }

    @Override
    public SubscriptionStatusResponseDto getMySubscription(Long userId) {
        // 1. 가장 최근 구독 조회 (ACTIVE, PAUSED, CANCELED, INACTIVE 중 최신 1건)
        // 2. 구독이 없으면 status: NONE 반환
        return subscriptionRepository.findTopByUserIdAndStatusInOrderByCreatedAtDesc(
                        userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED,
                                SubscriptionStatus.CANCELED, SubscriptionStatus.INACTIVE))
                .map(SubscriptionStatusResponseDto::from)
                .orElse(SubscriptionStatusResponseDto.none());
    }

    @Override
    @Transactional
    public SubscriptionCancelResponseDto cancelSubscription(Long userId) {
        // 1. ACTIVE 상태의 구독이 있는지 검증
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(SubscriptionNotFoundException::new);

        // 2. subscription UPDATE (status: CANCELED, canceledAt: 현재 시각)
        //    즉시 중단이 아닌 expiredAt까지 서비스 유지, 토스 빌링키 삭제 없음 (재구독 가능)
        subscription.cancel();

        return SubscriptionCancelResponseDto.from(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResumeResponseDto resumeSubscription(Long userId, SubscriptionResumeRequestDto request) {
        // 1. PAUSED 상태의 구독이 있는지 검증
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PAUSED)
                .orElseThrow(PausedSubscriptionNotFoundException::new);

        // 2. methodId가 본인 소유이고 활성 상태인지 검증
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndIsActiveTrue(request.getMethodId())
                .orElseThrow(PaymentMethodNotFoundException::new);

        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new PaymentMethodNotFoundException();
        }

        // 3. 사용자 조회 (토스 API 호출에 필요한 customerKey, email, name)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // 4. 밀린 결제 즉시 재시도 (READY 선저장 → chargeBilling → markDone/markFailed)
        billingExecutor.execute(user, paymentMethod, subscription, SUBSCRIPTION_ORDER_NAME, SUBSCRIPTION_PRICE);

        // 5. subscription UPDATE (status: ACTIVE, 새 결제 수단 연결, retryCount: 0, nextBillingAt: 현재 +1달)
        subscription.resume(LocalDateTime.now().plusMonths(1), paymentMethod);

        return SubscriptionResumeResponseDto.from(subscription);
    }

    @Override
    @Transactional
    public SubscriptionRefundResponseDto refundSubscription(Long userId) {
        // 1. ACTIVE 상태의 구독이 있는지 검증
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(SubscriptionNotFoundException::new);

        // 2. 결제일(startedAt)로부터 1일 이내인지 검증
        if (subscription.getStartedAt() == null ||
                subscription.getStartedAt().isBefore(LocalDateTime.now().minusDays(1))) {
            throw new RefundPeriodExpiredException();
        }

        // 3. 가장 최근 DONE 결제 이력에서 paymentKey 조회 (토스 취소 API 호출에 필요)
        PaymentHistory history = paymentHistoryRepository
                .findTopBySubscriptionIdAndStatusOrderByCreatedAtDesc(subscription.getId(), PaymentHistoryStatus.DONE)
                .orElseThrow(RefundFailedException::new);

        // 4. 토스페이먼츠 결제 취소 API 호출
        try {
            tossPaymentsClient.cancelPayment(
                    history.getPaymentKey(),
                    TossPaymentCancelRequest.builder()
                            .cancelReason("고객 요청 취소")
                            .build()
            );
        } catch (Exception e) {
            log.error("토스 환불 API 호출 실패: {}", e.getMessage());
            throw new RefundFailedException();
        }

        // 5. payment_history 상태 CANCELED로 업데이트
        history.markCanceled();

        // 6. subscription 상태 REFUNDED로 업데이트 (즉시 만료)
        subscription.refund();

        return SubscriptionRefundResponseDto.from(subscription);
    }
}
