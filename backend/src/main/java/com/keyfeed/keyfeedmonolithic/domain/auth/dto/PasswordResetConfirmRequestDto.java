package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmRequestDto {
    private String email;
    private String newPassword;
    private String confirmPassword;
}
