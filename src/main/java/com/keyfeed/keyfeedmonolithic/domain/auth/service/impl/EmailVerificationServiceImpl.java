package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerification;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerifyStatus;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAlreadyDoneException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAttemptLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationExpiredException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationLockedException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.EmailVerificationRepository;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.EmailVerificationService;
import com.keyfeed.keyfeedmonolithic.domain.auth.util.VerificationCodeUtil;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import com.keyfeed.keyfeedmonolithic.global.mail.EmailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    @Value("${spring.mail.lock_minutes}")
    private int lockMinutes;

    @Value("${spring.mail.expire_minutes}")
    private int expireMinutes;

    @Value("${spring.mail.max_attempts}")
    private int maxAttempts;

    private final EmailVerificationRepository emailVerificationRepository;
    private final SpringTemplateEngine templateEngine;
    private final EmailClient emailClient;

    @Override
    public void sendVerificationEmail(String email, EmailPurpose purpose, String subject) {
        String code = VerificationCodeUtil.generateEmailVerificationCode();
        final LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerification> optionalEmailVerification =
                emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose);

        if (optionalEmailVerification.isPresent()) {
            handleExistingEmailVerification(optionalEmailVerification.get(), code, now, purpose, subject);
            return;
        }

        saveNewEmailVerification(email, code, now, purpose);
        sendEmail(email, code, subject);
    }

    @Override
    public EmailVerificationConfirmResponseDto verifyCode(String email, String code, EmailPurpose purpose) {
        final LocalDateTime now = LocalDateTime.now();

        EmailVerification emailVerification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
                .orElseThrow(() -> new EntityNotFoundException("EmailVerification", email));

        if (emailVerification.getStatus().equals(EmailVerifyStatus.VERIFIED)) {
            log.info("이메일이 이미 인증되었습니다. {}", email);
            return EmailVerificationConfirmResponseDto.from(emailVerification);
        }

        if (emailVerification.getStatus() == EmailVerifyStatus.LOCKED) {
            LocalDateTime lockUntil = emailVerification.getLockedUntil();

            if (lockUntil != null && now.isBefore(lockUntil)) {
                throw new EmailVerificationLockedException();
            }

            emailVerification.updateStatus(EmailVerifyStatus.PENDING);
            emailVerification.resetAttemptCount();
        }

        if (emailVerification.getExpiresAt().isBefore(now)) {
            emailVerification.updateStatus(EmailVerifyStatus.EXPIRED);
            emailVerificationRepository.save(emailVerification);
            throw new EmailVerificationExpiredException();
        }

        if (emailVerification.getCode().equals(code)) {
            emailVerification.updateStatus(EmailVerifyStatus.VERIFIED);
            emailVerificationRepository.save(emailVerification);
            return EmailVerificationConfirmResponseDto.from(emailVerification);
        }

        emailVerification.increaseAttemptCount();

        if (emailVerification.getAttemptCount() >= maxAttempts) {
            emailVerification.updateStatus(EmailVerifyStatus.LOCKED);
            emailVerification.updateLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            emailVerificationRepository.save(emailVerification);

            throw new EmailVerificationAttemptLimitExceededException();
        }

        emailVerificationRepository.save(emailVerification);

        return EmailVerificationConfirmResponseDto.from(emailVerification);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(String email, EmailPurpose purpose) {
        return emailVerificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
                .map(ev -> ev.getStatus() == EmailVerifyStatus.VERIFIED)
                .orElse(false);
    }

    @Override
    public void deleteVerification(String email, EmailPurpose purpose) {
        emailVerificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
                .ifPresent(emailVerificationRepository::delete);
    }

    private void handleExistingEmailVerification(EmailVerification emailVerification, String code,
            LocalDateTime now, EmailPurpose purpose, String subject) {
        String email = emailVerification.getEmail();

        if (emailVerification.getStatus() == EmailVerifyStatus.VERIFIED) {
            throw new EmailVerificationAlreadyDoneException();
        } else if (emailVerification.getStatus() == EmailVerifyStatus.LOCKED) {
            handleLockedEmailVerification(emailVerification, code, now);
        } else if (emailVerification.getStatus() == EmailVerifyStatus.PENDING
                && emailVerification.getExpiresAt().isAfter(now)) {
            handleNotExpiredEmailVerification(emailVerification, code, now);
        } else if (emailVerification.getExpiresAt().isBefore(now)) {
            handleExpiredEmailVerification(emailVerification);
            saveNewEmailVerification(email, code, now, purpose);
        }

        emailVerificationRepository.save(emailVerification);
        sendEmail(email, code, subject);
    }

    private void handleLockedEmailVerification(EmailVerification emailVerification, String code, LocalDateTime now) {
        if (emailVerification.getLockedUntil() != null && now.isBefore(emailVerification.getLockedUntil())) {
            throw new EmailVerificationLockedException();
        }
        emailVerification.updateStatus(EmailVerifyStatus.PENDING);
        emailVerification.updateCode(code);
        emailVerification.resetAttemptCount();
        emailVerification.clearLockedUntil();
    }

    private void handleNotExpiredEmailVerification(EmailVerification emailVerification, String code, LocalDateTime now) {
        emailVerification.updateCode(code);
        emailVerification.resetExpiresAt(now.plusMinutes(expireMinutes));
    }

    private void handleExpiredEmailVerification(EmailVerification emailVerification) {
        emailVerification.updateStatus(EmailVerifyStatus.EXPIRED);
    }

    private String buildVerificationHtml(String email, String code) {
        Context context = new Context();
        context.setVariable("brandName", "Key Feed");
        context.setVariable("email", email);
        context.setVariable("code", code);
        context.setVariable("expiresMinutes", expireMinutes);
        context.setVariable("year", Year.now().getValue());

        return templateEngine.process("email/verification", context);
    }

    private void sendEmail(String email, String code, String subject) {
        String html = buildVerificationHtml(email, code);
        emailClient.sendOneEmail(email, subject, html);
    }

    private EmailVerification saveNewEmailVerification(String email, String code, LocalDateTime now, EmailPurpose purpose) {
        EmailVerification emailVerification = EmailVerification.builder()
                .email(email)
                .purpose(purpose)
                .code(code)
                .status(EmailVerifyStatus.PENDING)
                .attemptCount(0)
                .expiresAt(now.plusMinutes(expireMinutes))
                .build();
        return emailVerificationRepository.save(emailVerification);
    }
}
