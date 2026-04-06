package com.keyfeed.keyfeedmonolithic.domain.payment.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.ChargeResult;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistoryStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingChargeRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingExecutor {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final TossPaymentsClient tossPaymentsClient;

    /**
     * 공통 결제 실행 시퀀스:
     * READY history 선저장 → chargeBilling 호출 → 성공: markDone / 실패: markFailed 후 예외 rethrow
     *
     * @param subscription 신규 구독 시 null 허용 (startSubscription 케이스)
     */
    public ChargeResult execute(User user, PaymentMethod paymentMethod, Subscription subscription,
                                String orderName, int amount) {
        String orderId = generateOrderId(user.getId());

        PaymentHistory history = PaymentHistory.builder()
                .user(user)
                .paymentMethod(paymentMethod)
                .subscription(subscription)
                .orderId(orderId)
                .orderName(orderName)
                .amount(amount)
                .methodType(paymentMethod.getMethodType())
                .status(PaymentHistoryStatus.READY)
                .build();
        paymentHistoryRepository.save(history);

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
            history.markFailed(e.getMessage());
            throw e;
        }

        history.markDone(chargeResponse.getPaymentKey(), parseApprovedAt(chargeResponse.getApprovedAt()));
        return new ChargeResult(history, chargeResponse);
    }

    private String generateOrderId(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return String.format("%d-%s-%04d", userId, timestamp, random);
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
