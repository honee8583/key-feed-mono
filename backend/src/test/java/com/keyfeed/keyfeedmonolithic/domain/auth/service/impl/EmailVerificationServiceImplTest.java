package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerification;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerifyStatus;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAlreadyDoneException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationLockedException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.EmailVerificationRepository;
import com.keyfeed.keyfeedmonolithic.global.mail.EmailSendEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final String EMAIL = "test@keyfeed.com";
    private static final String SUBJECT = "[Key Feed] 인증번호";
    private static final String HTML = "<html>code</html>";
    private static final int EXPIRE_MINUTES = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "expireMinutes", EXPIRE_MINUTES);
        ReflectionTestUtils.setField(emailVerificationService, "lockMinutes", LOCK_MINUTES);
        ReflectionTestUtils.setField(emailVerificationService, "maxAttempts", MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("신규 인증 요청 시 인증 정보를 저장하고 메일 발송 이벤트를 발행한다")
    void 신규_인증_요청시_이벤트_발행() {
        // given
        given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(EMAIL, EmailPurpose.SIGNUP))
                .willReturn(Optional.empty());
        given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn(HTML);

        // when
        emailVerificationService.sendVerificationEmail(EMAIL, EmailPurpose.SIGNUP, SUBJECT);

        // then
        then(emailVerificationRepository).should().save(any(EmailVerification.class));
        then(eventPublisher).should().publishEvent(new EmailSendEvent(EMAIL, SUBJECT, HTML));
    }

    @Test
    @DisplayName("기존 PENDING(미만료) 인증이 있으면 코드를 갱신하고 메일 발송 이벤트를 발행한다")
    void 기존_미만료_인증_재요청시_이벤트_발행() {
        // given
        EmailVerification existing = EmailVerification.builder()
                .email(EMAIL)
                .purpose(EmailPurpose.SIGNUP)
                .code("000000")
                .status(EmailVerifyStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES))
                .build();
        given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(EMAIL, EmailPurpose.SIGNUP))
                .willReturn(Optional.of(existing));
        given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn(HTML);

        // when
        emailVerificationService.sendVerificationEmail(EMAIL, EmailPurpose.SIGNUP, SUBJECT);

        // then
        then(eventPublisher).should().publishEvent(new EmailSendEvent(EMAIL, SUBJECT, HTML));
    }

    @Test
    @DisplayName("이미 인증 완료된 이메일이면 예외를 던지고 메일 발송 이벤트를 발행하지 않는다")
    void 이미_인증완료시_이벤트_미발행() {
        // given
        EmailVerification verified = EmailVerification.builder()
                .email(EMAIL)
                .purpose(EmailPurpose.SIGNUP)
                .status(EmailVerifyStatus.VERIFIED)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES))
                .build();
        given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(EMAIL, EmailPurpose.SIGNUP))
                .willReturn(Optional.of(verified));

        // when & then
        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationEmail(EMAIL, EmailPurpose.SIGNUP, SUBJECT))
                .isInstanceOf(EmailVerificationAlreadyDoneException.class);

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("잠금 상태이고 잠금 시간이 지나지 않았으면 예외를 던지고 메일 발송 이벤트를 발행하지 않는다")
    void 잠금상태시_이벤트_미발행() {
        // given
        EmailVerification locked = EmailVerification.builder()
                .email(EMAIL)
                .purpose(EmailPurpose.SIGNUP)
                .status(EmailVerifyStatus.LOCKED)
                .lockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES))
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES))
                .build();
        given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(EMAIL, EmailPurpose.SIGNUP))
                .willReturn(Optional.of(locked));

        // when & then
        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationEmail(EMAIL, EmailPurpose.SIGNUP, SUBJECT))
                .isInstanceOf(EmailVerificationLockedException.class);

        then(eventPublisher).should(never()).publishEvent(any());
    }
}
