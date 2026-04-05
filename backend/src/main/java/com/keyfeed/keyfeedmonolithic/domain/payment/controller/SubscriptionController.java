package com.keyfeed.keyfeedmonolithic.domain.payment.controller;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.SubscriptionService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 구독 시작
    @PostMapping("/start")
    public ResponseEntity<HttpResponse> startSubscription(@AuthenticationPrincipal Long userId,
                                                          @Valid @RequestBody SubscriptionStartRequestDto request) {
        SubscriptionStartResponseDto result = subscriptionService.startSubscription(userId, request);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.SUBSCRIPTION_STARTED.getMessage(), result));
    }

    // 내 구독 조회
    @GetMapping("/me")
    public ResponseEntity<HttpResponse> getMySubscription(@AuthenticationPrincipal Long userId) {
        SubscriptionStatusResponseDto result = subscriptionService.getMySubscription(userId);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.SUBSCRIPTION_STATUS_READ.getMessage(), result));
    }

    // 구독 해지 (만료일까지 서비스 유지)
    @PostMapping("/cancel")
    public ResponseEntity<HttpResponse> cancelSubscription(@AuthenticationPrincipal Long userId) {
        SubscriptionCancelResponseDto result = subscriptionService.cancelSubscription(userId);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.SUBSCRIPTION_CANCELED.getMessage(), result));
    }

    // 구독 재개
    @PostMapping("/resume")
    public ResponseEntity<HttpResponse> resumeSubscription(@AuthenticationPrincipal Long userId,
                                                           @Valid @RequestBody SubscriptionResumeRequestDto request) {
        SubscriptionResumeResponseDto result = subscriptionService.resumeSubscription(userId, request);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.SUBSCRIPTION_RESUMED.getMessage(), result));
    }

    // 구독 취소 (결제일 1일 이내 즉시 환불)
    @PostMapping("/refund")
    public ResponseEntity<HttpResponse> refundSubscription(@AuthenticationPrincipal Long userId) {
        SubscriptionRefundResponseDto result = subscriptionService.refundSubscription(userId);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.SUBSCRIPTION_REFUNDED.getMessage(), result));
    }
}
