package com.keyfeed.keyfeedmonolithic.domain.payment.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.ChargeResult;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.writer.PaymentHistoryWriter;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingChargeRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingExecutor {

    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentHistoryWriter paymentHistoryWriter;

    public ChargeResult execute(User user, PaymentMethod paymentMethod, Subscription subscription,
                                String orderName, int amount) {
        String orderId = generateOrderId(user.getId());

        // 1. 고유한 orderId로 결제 내역 선저장
        // 결제에 실패한 경우 기존 orderId로 재결제 시도로 중복 방지
        PaymentHistory history = paymentHistoryWriter.saveReady(
                user, paymentMethod, subscription, orderId, orderName, amount
        );

        // 2. orderId로 결제 진행
        TossBillingChargeResponse chargeResponse;
        try {
            chargeResponse = tossPaymentsClient.chargeBilling(
                    paymentMethod.getBillingKey(),
                    TossBillingChargeRequest.builder()
                            .customerKey(user.getCustomerKey())
                            .amount(amount)
                            .orderId(orderId)
                            .orderName(orderName)
                            .customerEmail(user.getEmail())
                            .customerName(user.getUsername())
                            .taxFreeAmount(0)
                            .build()
            );
        } catch (Exception e) {
            paymentHistoryWriter.updateFailed(history, e.getMessage());
            throw e;
        }

        // 3. 결제내역 상태 업데이트
        paymentHistoryWriter.updateDone(
                history,
                chargeResponse.getPaymentKey(),
                parseApprovedAt(chargeResponse.getApprovedAt())
        );

        return new ChargeResult(history, chargeResponse);
    }

    private String generateOrderId(Long userId) {
        return "billing-" + userId + "-" + UUID.randomUUID();
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (approvedAt == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(approvedAt).toLocalDateTime();
        } catch (Exception e) {
            log.warn("approvedAt 파싱 실패: {}", approvedAt);
            return null;
        }
    }
}
