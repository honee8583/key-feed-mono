package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.JoinRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.UserAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.EmailVerificationService;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.JoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JoinServiceImpl implements JoinService {

    private static final String SIGNUP_EMAIL_SUBJECT = "[Key Feed] 회원가입을 위한 이메일 인증번호입니다.";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public void join(JoinRequestDto joinRequestDto) {
        userRepository.findByEmail(joinRequestDto.getEmail())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException();
                });

        User user = joinRequestDto.toEntity(passwordEncoder.encode(joinRequestDto.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void sendJoinEmail(String email) {
        emailVerificationService.sendVerificationEmail(email, EmailPurpose.SIGNUP, SIGNUP_EMAIL_SUBJECT);
    }

    @Override
    public EmailVerificationConfirmResponseDto verifyEmailCode(EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto) {
        return emailVerificationService.verifyCode(
                emailVerificationConfirmRequestDto.getEmail(),
                emailVerificationConfirmRequestDto.getCode(),
                EmailPurpose.SIGNUP
        );
    }
}
