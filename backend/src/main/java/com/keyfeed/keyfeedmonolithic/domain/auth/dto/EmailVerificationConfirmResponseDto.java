package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerification;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerifyStatus;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationConfirmResponseDto {
    private EmailVerifyStatus status;  // 인증상태
    private int attempts;              // 시도횟수
    private LocalDateTime retryAt;     // 잠금 기간
    private LocalDateTime expiresAt;   // 만료 기간

    public static EmailVerificationConfirmResponseDto from(EmailVerification emailVerification) {
        return EmailVerificationConfirmResponseDto.builder()
                .status(emailVerification.getStatus())
                .attempts(emailVerification.getAttemptCount())
                .retryAt(emailVerification.getLockedUntil())
                .expiresAt(emailVerification.getExpiresAt())
                .build();
    }
}
