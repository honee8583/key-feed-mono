package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerifyStatus;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAlreadyDoneException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationAttemptLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationCooldownException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationExpiredException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailVerificationLockedException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.EmailVerificationCacheRepository;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    @Mock
    private EmailVerificationCacheRepository cacheRepository;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final String EMAIL = "test@keyfeed.com";
    private static final String SUBJECT = "[Key Feed] 인증번호";
    private static final String HTML = "<html>code</html>";
    private static final String CODE = "473829";
    private static final EmailPurpose PURPOSE = EmailPurpose.SIGNUP;
    private static final int EXPIRE_MINUTES = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int DONE_MINUTES = 10;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "expireMinutes", EXPIRE_MINUTES);
        ReflectionTestUtils.setField(emailVerificationService, "lockMinutes", LOCK_MINUTES);
        ReflectionTestUtils.setField(emailVerificationService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(emailVerificationService, "cooldownSeconds", COOLDOWN_SECONDS);
        ReflectionTestUtils.setField(emailVerificationService, "doneMinutes", DONE_MINUTES);
    }

    // ===================== sendVerificationEmail =====================

    @Test
    @DisplayName("발송 가능한 상태면 코드를 저장하고 메일 발송 이벤트를 발행한다")
    void 신규_인증_요청시_코드저장_및_이벤트발행() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.tryCooldown(eq(PURPOSE), eq(EMAIL), any(Duration.class))).willReturn(true);
        given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn(HTML);

        // when
        emailVerificationService.sendVerificationEmail(EMAIL, PURPOSE, SUBJECT);

        // then
        then(cacheRepository).should()
                .saveCode(eq(PURPOSE), eq(EMAIL), anyString(), any(Duration.class));
        then(eventPublisher).should().publishEvent(new EmailSendEvent(EMAIL, SUBJECT, HTML));
    }

    @Test
    @DisplayName("잠금 상태면 예외를 던지고 코드 저장/메일 발송을 하지 않는다")
    void 잠금상태_발송_차단() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(EMAIL, PURPOSE, SUBJECT))
                .isInstanceOf(EmailVerificationLockedException.class);

        then(cacheRepository).should(never()).saveCode(any(), any(), any(), any());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("이미 인증 완료된 이메일이면 예외를 던지고 메일 발송을 하지 않는다")
    void 인증완료_발송_차단() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(EMAIL, PURPOSE, SUBJECT))
                .isInstanceOf(EmailVerificationAlreadyDoneException.class);

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("쿨다운 중이면 예외를 던지고 메일 발송을 하지 않는다")
    void 쿨다운_발송_차단() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.tryCooldown(eq(PURPOSE), eq(EMAIL), any(Duration.class))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(EMAIL, PURPOSE, SUBJECT))
                .isInstanceOf(EmailVerificationCooldownException.class);

        then(cacheRepository).should(never()).saveCode(any(), any(), any(), any());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    // ===================== verifyCode =====================

    @Test
    @DisplayName("코드가 일치하면 완료 키를 저장하고 코드 키를 삭제하며 VERIFIED를 반환한다")
    void 코드일치시_인증완료() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.getCode(PURPOSE, EMAIL)).willReturn(CODE);

        // when
        EmailVerificationConfirmResponseDto response =
                emailVerificationService.verifyCode(EMAIL, CODE, PURPOSE);

        // then
        assertThat(response.getStatus()).isEqualTo(EmailVerifyStatus.VERIFIED);
        then(cacheRepository).should().markDone(eq(PURPOSE), eq(EMAIL), any(Duration.class));
        then(cacheRepository).should().deleteCode(PURPOSE, EMAIL);
    }

    @Test
    @DisplayName("이미 완료된 인증이면 멱등하게 VERIFIED를 반환한다")
    void 완료된_인증_멱등반환() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(true);

        // when
        EmailVerificationConfirmResponseDto response =
                emailVerificationService.verifyCode(EMAIL, CODE, PURPOSE);

        // then
        assertThat(response.getStatus()).isEqualTo(EmailVerifyStatus.VERIFIED);
        then(cacheRepository).should(never()).getCode(any(), any());
    }

    @Test
    @DisplayName("잠금 상태면 검증 시 예외를 던진다")
    void 잠금상태_검증_차단() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, CODE, PURPOSE))
                .isInstanceOf(EmailVerificationLockedException.class);
    }

    @Test
    @DisplayName("코드 키가 없으면(TTL 만료) 만료 예외를 던진다")
    void 코드_만료시_예외() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.getCode(PURPOSE, EMAIL)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, CODE, PURPOSE))
                .isInstanceOf(EmailVerificationExpiredException.class);
    }

    @Test
    @DisplayName("코드가 불일치하고 최대 시도 미만이면 시도횟수를 올리고 PENDING을 반환한다")
    void 코드불일치_시도증가_PENDING() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.getCode(PURPOSE, EMAIL)).willReturn(CODE);
        given(cacheRepository.increaseAttempt(PURPOSE, EMAIL)).willReturn(2L);
        given(cacheRepository.getCodeTtlSeconds(PURPOSE, EMAIL)).willReturn(180L);

        // when
        EmailVerificationConfirmResponseDto response =
                emailVerificationService.verifyCode(EMAIL, "000000", PURPOSE);

        // then
        assertThat(response.getStatus()).isEqualTo(EmailVerifyStatus.PENDING);
        assertThat(response.getAttempts()).isEqualTo(2);
        then(cacheRepository).should(never()).lock(any(), any(), any());
    }

    @Test
    @DisplayName("시도횟수가 최대치에 도달하면 잠금 후 시도초과 예외를 던진다")
    void 시도초과시_잠금_및_예외() {
        // given
        given(cacheRepository.isLocked(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.isDone(PURPOSE, EMAIL)).willReturn(false);
        given(cacheRepository.getCode(PURPOSE, EMAIL)).willReturn(CODE);
        given(cacheRepository.increaseAttempt(PURPOSE, EMAIL)).willReturn((long) MAX_ATTEMPTS);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "000000", PURPOSE))
                .isInstanceOf(EmailVerificationAttemptLimitExceededException.class);

        then(cacheRepository).should().lock(eq(PURPOSE), eq(EMAIL), any(Duration.class));
        then(cacheRepository).should().deleteCode(PURPOSE, EMAIL);
    }

    // ===================== isVerified / deleteVerification =====================

    @Test
    @DisplayName("isVerified는 done 키 존재 여부를 반환한다")
    void isVerified_done키_위임() {
        // given
        given(cacheRepository.isDone(EmailPurpose.RESET, EMAIL)).willReturn(true);

        // when & then
        assertThat(emailVerificationService.isVerified(EMAIL, EmailPurpose.RESET)).isTrue();
    }

    @Test
    @DisplayName("deleteVerification은 done 키를 삭제한다")
    void deleteVerification_done키_삭제() {
        // when
        emailVerificationService.deleteVerification(EMAIL, EmailPurpose.RESET);

        // then
        then(cacheRepository).should().deleteDone(EmailPurpose.RESET, EMAIL);
    }
}
