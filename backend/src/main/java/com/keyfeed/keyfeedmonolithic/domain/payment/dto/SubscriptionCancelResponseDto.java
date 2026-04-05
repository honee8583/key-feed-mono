package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class SubscriptionCancelResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private String status;
    private String expiredAt;
    private String canceledAt;

    public static SubscriptionCancelResponseDto from(Subscription sub) {
        return SubscriptionCancelResponseDto.builder()
                .status(sub.getStatus().name())
                .expiredAt(sub.getExpiredAt() != null ? sub.getExpiredAt().format(FORMATTER) : null)
                .canceledAt(sub.getCanceledAt() != null ? sub.getCanceledAt().format(FORMATTER) : null)
                .build();
    }
}
