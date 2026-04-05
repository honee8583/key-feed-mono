package com.keyfeed.keyfeedmonolithic.domain.payment.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodRegisterRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethodType;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.DuplicatePaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentMethodAccessDeniedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentMethodInUseException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentMethodNotFoundException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentMethodRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.PaymentMethodService;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingIssueRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingIssueResponse;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TossPaymentsClient tossPaymentsClient;

    @Override
    public String getOrCreateCustomerKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (user.getCustomerKey() == null) {
            user.updateCustomerKey(UUID.randomUUID().toString());
            userRepository.save(user);
        }

        return user.getCustomerKey();
    }

    @Override
    public PaymentMethodResponseDto registerPaymentMethod(Long userId, PaymentMethodRegisterRequestDto registerRequest) {
        // 1. 사용자 조회 및 customerKey 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // 2. billingKey 발급
        TossBillingIssueResponse response = tossPaymentsClient.issueBillingKey(
                TossBillingIssueRequest.builder()
                        .authKey(registerRequest.getAuthKey())
                        .customerKey(user.getCustomerKey())
                        .build()
        );

        // 3. 카드 번호 추출
        String displayNumber = null;
        if (response.getCard() != null) {
            displayNumber = response.getCard().getNumber();
        }

        // 4. 동일 카드 중복 검증
        if (paymentMethodRepository.existsByUserIdAndDisplayNumberAndIsActiveTrue(userId, displayNumber)) {
            throw new DuplicatePaymentMethodException();
        }

        // 5. 첫 번째 카드라면 기본 결제 수단으로 설정
        boolean isFirst = paymentMethodRepository.countByUserIdAndIsActiveTrue(userId) == 0;

        // 6. 결제 수단 저장
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .billingKey(response.getBillingKey())
                .methodType(PaymentMethodType.CARD)
                .providerName(response.getCardCompany())
                .displayNumber(displayNumber)
                .isDefault(isFirst)
                .isActive(true)
                .build();
        paymentMethodRepository.save(paymentMethod);

        return PaymentMethodResponseDto.from(paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponseDto> getPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(PaymentMethodResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public void deletePaymentMethod(Long userId, Long methodId) {
        // 1. 결제 수단 조회 및 소유자 검증
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndIsActiveTrue(methodId)
                .orElseThrow(PaymentMethodNotFoundException::new);

        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new PaymentMethodAccessDeniedException();
        }

        // 2. 활성 구독에 연결된 수단인지 검증
        if (subscriptionRepository.existsByPaymentMethodIdAndStatus(methodId, SubscriptionStatus.ACTIVE)) {
            throw new PaymentMethodInUseException();
        }

        // 3. 토스 빌링키 삭제 (실패 시 DB 삭제 진행하지 않음)
        tossPaymentsClient.deleteBillingKey(paymentMethod.getBillingKey());

        // 4. 소프트 삭제
        boolean wasDefault = paymentMethod.isDefault();
        paymentMethod.softDelete();

        // 5. 기본 수단이었다면 차순위 수단을 기본으로 자동 지정
        if (wasDefault) {
            paymentMethodRepository.findTopByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
                    .ifPresent(PaymentMethod::setAsDefault);
        }
    }

    @Override
    public void changeDefaultPaymentMethod(Long userId, Long methodId) {
        // 1. 결제 수단 조회 및 소유자 검증
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndIsActiveTrue(methodId)
                .orElseThrow(PaymentMethodNotFoundException::new);

        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new PaymentMethodAccessDeniedException();
        }

        // 2. 이미 기본 수단이면 멱등성 보장 (변경 없이 반환)
        if (paymentMethod.isDefault()) {
            return;
        }

        // 3. 기존 기본 수단 해제 후 새 기본 수단 설정
        paymentMethodRepository
                .findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                .ifPresent(PaymentMethod::unsetDefault);

        paymentMethod.setAsDefault();

        // 4. 구독의 결제 수단 업데이트 (PAUSED 구독은 ACTIVE로 복구)
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndStatusIn(
                userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED));

        for (Subscription subscription : subscriptions) {
            if (subscription.getStatus() == SubscriptionStatus.PAUSED) {
                subscription.resume(subscription.getNextBillingAt(), paymentMethod);
            } else {
                subscription.updatePaymentMethod(paymentMethod);
            }
        }
    }
}
