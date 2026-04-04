package com.keyfeed.keyfeedmonolithic.domain.payment.controller;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodRegisterRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.PaymentMethodService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    // 고객 키 발급 (토스 SDK 호출 전 프론트에서 요청)
    @GetMapping("/customer-key")
    public ResponseEntity<HttpResponse> getCustomerKey(@AuthenticationPrincipal Long userId) {
        String customerKey = paymentMethodService.getOrCreateCustomerKey(userId);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.CUSTOMER_KEY_ISSUED.getMessage(), customerKey));
    }

    // 결제 수단 등록
    @PostMapping("/register")
    public ResponseEntity<HttpResponse> registerPaymentMethod(@AuthenticationPrincipal Long userId,
                                                              @Valid @RequestBody PaymentMethodRegisterRequestDto dto) {
        PaymentMethodResponseDto result = paymentMethodService.registerPaymentMethod(userId, dto);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.PAYMENT_METHOD_REGISTERED.getMessage(), result));
    }

    // 결제 수단 목록 조회
    @GetMapping
    public ResponseEntity<HttpResponse> getPaymentMethods(@AuthenticationPrincipal Long userId) {
        List<PaymentMethodResponseDto> result = paymentMethodService.getPaymentMethods(userId);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.PAYMENT_METHOD_LIST.getMessage(), result));
    }

    // 결제 수단 삭제
    @DeleteMapping("/{methodId}")
    public ResponseEntity<Void> deletePaymentMethod(@AuthenticationPrincipal Long userId,
                                                    @PathVariable Long methodId) {
        paymentMethodService.deletePaymentMethod(userId, methodId);
        return ResponseEntity.noContent().build();
    }

    // 기본 결제 수단 변경
    @PatchMapping("/{methodId}/default")
    public ResponseEntity<HttpResponse> changeDefaultPaymentMethod(@AuthenticationPrincipal Long userId,
                                                                   @PathVariable Long methodId) {
        paymentMethodService.changeDefaultPaymentMethod(userId, methodId);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.PAYMENT_METHOD_DEFAULT_CHANGED.getMessage(), null));
    }
}
