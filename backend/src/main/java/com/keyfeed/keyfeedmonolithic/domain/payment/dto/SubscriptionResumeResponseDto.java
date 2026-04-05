package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class SubscriptionResumeResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long subscriptionId;
    private String status;
    private String nextBillingAt;

    public static SubscriptionResumeResponseDto from(Subscription sub) {
        return SubscriptionResumeResponseDto.builder()
                .subscriptionId(sub.getId())
                .status(sub.getStatus().name())
                .nextBillingAt(sub.getNextBillingAt() != null ? sub.getNextBillingAt().format(FORMATTER) : null)
                .build();
    }
}
