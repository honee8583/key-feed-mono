package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDto {
    private String email;
}
