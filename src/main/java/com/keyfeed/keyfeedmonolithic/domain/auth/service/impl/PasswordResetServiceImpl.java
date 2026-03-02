package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordResetConfirmRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordResetRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordResetVerifyRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationRequiredException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.PasswordMismatchException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.SamePasswordException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.EmailVerificationService;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.PasswordResetService;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String PASSWORD_RESET_EMAIL_SUBJECT = "[Key Feed] 비밀번호 재설정을 위한 인증번호입니다.";

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void requestPasswordReset(PasswordResetRequestDto requestDto) {
        String email = requestDto.getEmail();

        // 등록된 사용자인지 확인
        userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        emailVerificationService.sendVerificationEmail(email, EmailPurpose.RESET, PASSWORD_RESET_EMAIL_SUBJECT);
    }

    @Override
    public EmailVerificationConfirmResponseDto verifyCode(PasswordResetVerifyRequestDto requestDto) {
        return emailVerificationService.verifyCode(
                requestDto.getEmail(),
                requestDto.getCode(),
                EmailPurpose.RESET
        );
    }

    @Override
    public void resetPassword(PasswordResetConfirmRequestDto requestDto) {
        String email = requestDto.getEmail();
        String newPassword = requestDto.getNewPassword();
        String confirmPassword = requestDto.getConfirmPassword();

        // 비밀번호 일치 확인
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException();
        }

        // 이메일 인증 완료 여부 확인
        if (!emailVerificationService.isVerified(email, EmailPurpose.RESET)) {
            throw new EmailVerificationRequiredException();
        }

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        // 기존 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new SamePasswordException();
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 인증 레코드 삭제
        emailVerificationService.deleteVerification(email, EmailPurpose.RESET);

        log.info("비밀번호 재설정 완료: {}", email);
    }
}
