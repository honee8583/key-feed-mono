package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentMethodRegisterRequestDto {

    @NotBlank
    private String authKey;
}
