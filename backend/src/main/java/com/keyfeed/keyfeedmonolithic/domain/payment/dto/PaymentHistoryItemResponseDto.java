package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class PaymentHistoryItemResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long paymentId;
    private String orderId;
    private String orderName;
    private int amount;
    private String status;
    private String failReason;
    private String approvedAt;
    private String createdAt;
    private PaymentMethodInfo paymentMethod;

    @Getter
    @Builder
    public static class PaymentMethodInfo {
        private String providerName;
        private String displayNumber;
        private String methodType;
    }

    public static PaymentHistoryItemResponseDto from(PaymentHistory history) {
        PaymentMethodInfo methodInfo = null;
        if (history.getPaymentMethod() != null) {
            methodInfo = PaymentMethodInfo.builder()
                    .providerName(history.getPaymentMethod().getProviderName())
                    .displayNumber(history.getPaymentMethod().getDisplayNumber())
                    .methodType(history.getMethodType() != null ? history.getMethodType().name() : null)
                    .build();
        }

        return PaymentHistoryItemResponseDto.builder()
                .paymentId(history.getId())
                .orderId(history.getOrderId())
                .orderName(history.getOrderName())
                .amount(history.getAmount())
                .status(history.getStatus().name())
                .failReason(history.getFailReason())
                .approvedAt(history.getApprovedAt() != null ? history.getApprovedAt().format(FORMATTER) : null)
                .createdAt(history.getCreatedAt() != null ? history.getCreatedAt().format(FORMATTER) : null)
                .paymentMethod(methodInfo)
                .build();
    }
}
