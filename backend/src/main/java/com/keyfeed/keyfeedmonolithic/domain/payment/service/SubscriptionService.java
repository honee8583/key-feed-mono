package com.keyfeed.keyfeedmonolithic.domain.payment.service;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.SubscriptionRefundResponseDto;

public interface SubscriptionService {

    SubscriptionStartResponseDto startSubscription(Long userId, SubscriptionStartRequestDto request);

    SubscriptionStatusResponseDto getMySubscription(Long userId);

    SubscriptionCancelResponseDto cancelSubscription(Long userId);

    SubscriptionResumeResponseDto resumeSubscription(Long userId, SubscriptionResumeRequestDto request);

    SubscriptionRefundResponseDto refundSubscription(Long userId);
}
