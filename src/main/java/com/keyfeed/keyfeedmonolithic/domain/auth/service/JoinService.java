package com.keyfeed.keyfeedmonolithic.domain.auth.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.JoinRequestDto;

public interface JoinService {

    void join(JoinRequestDto joinRequestDto);

    void sendJoinEmail(String email);

    EmailVerificationConfirmResponseDto verifyEmailCode(EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto);

}
