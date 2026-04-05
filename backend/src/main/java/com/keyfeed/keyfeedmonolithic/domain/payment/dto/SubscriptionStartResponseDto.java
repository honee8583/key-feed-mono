package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class SubscriptionStartResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long subscriptionId;
    private String status;
    private int price;
    private String nextBillingAt;
    private String expiredAt;

    public static SubscriptionStartResponseDto from(Subscription sub, PaymentHistory history) {
        return SubscriptionStartResponseDto.builder()
                .subscriptionId(sub.getId())
                .status(sub.getStatus().name())
                .price(sub.getPrice())
                .nextBillingAt(sub.getNextBillingAt() != null ? sub.getNextBillingAt().format(FORMATTER) : null)
                .expiredAt(sub.getExpiredAt() != null ? sub.getExpiredAt().format(FORMATTER) : null)
                .build();
    }
}
