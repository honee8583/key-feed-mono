package com.keyfeed.keyfeedmonolithic.domain.auth.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;

public interface EmailVerificationService {

    void sendVerificationEmail(String email, EmailPurpose purpose, String subject);

    EmailVerificationConfirmResponseDto verifyCode(String email, String code, EmailPurpose purpose);

    boolean isVerified(String email, EmailPurpose purpose);

    void deleteVerification(String email, EmailPurpose purpose);
}
