package com.keyfeed.keyfeedmonolithic.domain.auth.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordResetConfirmRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordResetRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordResetVerifyRequestDto;

public interface PasswordResetService {

    void requestPasswordReset(PasswordResetRequestDto requestDto);

    EmailVerificationConfirmResponseDto verifyCode(PasswordResetVerifyRequestDto requestDto);

    void resetPassword(PasswordResetConfirmRequestDto requestDto);
}
