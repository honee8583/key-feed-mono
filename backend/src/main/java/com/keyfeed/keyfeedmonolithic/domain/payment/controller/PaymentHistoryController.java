package com.keyfeed.keyfeedmonolithic.domain.payment.controller;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentHistoryItemResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.PaymentHistoryService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-history")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    @GetMapping
    public ResponseEntity<HttpResponse> getPaymentHistory(@AuthenticationPrincipal Long userId,
                                                          @RequestParam(required = false) Long cursorId,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) String status) {
        CursorPage<PaymentHistoryItemResponseDto> result = paymentHistoryService.getPaymentHistory(userId, cursorId, size, status);
        return ResponseEntity.ok(new HttpResponse(HttpStatus.OK, SuccessMessage.PAYMENT_HISTORY_LIST.getMessage(), result));
    }
}
