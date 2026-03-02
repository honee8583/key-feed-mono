package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationConfirmRequestDto {
    private String email;
    private String code;
}
