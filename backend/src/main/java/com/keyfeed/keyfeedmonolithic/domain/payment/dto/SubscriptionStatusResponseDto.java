package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class SubscriptionStatusResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long subscriptionId;
    private String status;
    private Integer price;
    private String startedAt;
    private String nextBillingAt;
    private String expiredAt;
    private String canceledAt;
    private Integer retryCount;
    private String providerName;
    private String displayNumber;

    public static SubscriptionStatusResponseDto none() {
        return SubscriptionStatusResponseDto.builder()
                .status("NONE")
                .build();
    }

    public static SubscriptionStatusResponseDto from(Subscription sub) {
        String providerName = null;
        String displayNumber = null;
        if (sub.getPaymentMethod() != null) {
            providerName = sub.getPaymentMethod().getProviderName();
            displayNumber = sub.getPaymentMethod().getDisplayNumber();
        }

        return SubscriptionStatusResponseDto.builder()
                .subscriptionId(sub.getId())
                .status(sub.getStatus().name())
                .price(sub.getPrice())
                .startedAt(sub.getStartedAt() != null ? sub.getStartedAt().format(FORMATTER) : null)
                .nextBillingAt(sub.getNextBillingAt() != null ? sub.getNextBillingAt().format(FORMATTER) : null)
                .expiredAt(sub.getExpiredAt() != null ? sub.getExpiredAt().format(FORMATTER) : null)
                .canceledAt(sub.getCanceledAt() != null ? sub.getCanceledAt().format(FORMATTER) : null)
                .retryCount(sub.getRetryCount())
                .providerName(providerName)
                .displayNumber(displayNumber)
                .build();
    }
}
