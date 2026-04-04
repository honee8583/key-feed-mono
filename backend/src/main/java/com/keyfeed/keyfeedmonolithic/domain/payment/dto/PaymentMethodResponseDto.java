package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class PaymentMethodResponseDto {
    private Long methodId;
    private String methodType;
    private String providerName;
    private String displayNumber;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private String createdAt;

    public static PaymentMethodResponseDto from(PaymentMethod paymentMethod) {
        return PaymentMethodResponseDto.builder()
                .methodId(paymentMethod.getId())
                .methodType(paymentMethod.getMethodType().name())
                .providerName(paymentMethod.getProviderName())
                .displayNumber(paymentMethod.getDisplayNumber())
                .isDefault(paymentMethod.isDefault())
                .createdAt(paymentMethod.getCreatedAt() != null
                        ? paymentMethod.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .build();
    }
}
