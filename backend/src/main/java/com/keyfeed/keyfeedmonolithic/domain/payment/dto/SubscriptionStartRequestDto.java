package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionStartRequestDto {

    @NotNull
    private Long methodId;
}
