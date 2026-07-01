package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAlreadyDoneException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAttemptLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationCooldownException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationExpiredException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationLockedException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.EmailVerificationCacheRepository;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.EmailVerificationService;
import com.keyfeed.keyfeedmonolithic.domain.auth.util.VerificationCodeUtil;
import com.keyfeed.keyfeedmonolithic.global.mail.EmailSendEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    @Value("${spring.mail.lock_minutes}")
    private int lockMinutes;

    @Value("${spring.mail.expire_minutes}")
    private int expireMinutes;

    @Value("${spring.mail.max_attempts}")
    private int maxAttempts;

    @Value("${spring.mail.cooldown_seconds}")
    private int cooldownSeconds;

    @Value("${spring.mail.done_minutes}")
    private int doneMinutes;

    private final EmailVerificationCacheRepository cacheRepository;
    private final SpringTemplateEngine templateEngine;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 인증 코드를 발급/교체하고 메일 발송 이벤트를 발행한다.
     *
     * <p>메일 발송은 {@code @TransactionalEventListener(AFTER_COMMIT)}에 의존하므로,
     * ambient 트랜잭션 커밋 시 발화하도록 {@code @Transactional}을 유지한다.
     */
    @Override
    @Transactional
    public void sendVerificationEmail(String email, EmailPurpose purpose, String subject) {
        if (cacheRepository.isLocked(purpose, email)) {
            throw new EmailVerificationLockedException();
        }
        if (cacheRepository.isDone(purpose, email)) {
            throw new EmailVerificationAlreadyDoneException();
        }
        if (!cacheRepository.tryCooldown(purpose, email, Duration.ofSeconds(cooldownSeconds))) {
            throw new EmailVerificationCooldownException();
        }

        String code = VerificationCodeUtil.generateEmailVerificationCode();
        cacheRepository.saveCode(purpose, email, code, Duration.ofMinutes(expireMinutes));

        sendEmail(email, code, subject);
    }

    @Override
    @Transactional
    public EmailVerificationConfirmResponseDto verifyCode(String email, String code, EmailPurpose purpose) {
        if (cacheRepository.isLocked(purpose, email)) {
            throw new EmailVerificationLockedException();
        }

        if (cacheRepository.isDone(purpose, email)) {
            log.info("이메일이 이미 인증되었습니다. {}", email);
            return EmailVerificationConfirmResponseDto.verified();
        }

        String savedCode = cacheRepository.getCode(purpose, email);
        if (savedCode == null) {
            throw new EmailVerificationExpiredException();
        }

        if (savedCode.equals(code)) {
            cacheRepository.markDone(purpose, email, Duration.ofMinutes(doneMinutes));
            cacheRepository.deleteCode(purpose, email);
            return EmailVerificationConfirmResponseDto.verified();
        }

        long attempts = cacheRepository.increaseAttempt(purpose, email);
        if (attempts >= maxAttempts) {
            cacheRepository.lock(purpose, email, Duration.ofMinutes(lockMinutes));
            cacheRepository.deleteCode(purpose, email);
            throw new EmailVerificationAttemptLimitExceededException();
        }

        long ttlSeconds = cacheRepository.getCodeTtlSeconds(purpose, email);
        return EmailVerificationConfirmResponseDto.pending((int) attempts, LocalDateTime.now().plusSeconds(ttlSeconds));
    }

    @Override
    public boolean isVerified(String email, EmailPurpose purpose) {
        return cacheRepository.isDone(purpose, email);
    }

    @Override
    public void deleteVerification(String email, EmailPurpose purpose) {
        cacheRepository.deleteDone(purpose, email);
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
        eventPublisher.publishEvent(new EmailSendEvent(email, subject, html));
    }
}
