package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class SubscriptionRefundResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long subscriptionId;
    private String status;
    private String canceledAt;

    public static SubscriptionRefundResponseDto from(Subscription sub) {
        return SubscriptionRefundResponseDto.builder()
                .subscriptionId(sub.getId())
                .status(sub.getStatus().name())
                .canceledAt(sub.getCanceledAt() != null ? sub.getCanceledAt().format(FORMATTER) : null)
                .build();
    }
}
