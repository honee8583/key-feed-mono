package com.keyfeed.keyfeedmonolithic.domain.auth.controller;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.*;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailVerifyStatus;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.RefreshTokenNotExistsException;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.JoinService;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.LoginService;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.PasswordResetService;
import com.keyfeed.keyfeedmonolithic.domain.auth.util.CookieUtil;
import com.keyfeed.keyfeedmonolithic.global.auth.jwt.JwtProperties;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final JoinService joinService;
    private final PasswordResetService passwordResetService;
    private final JwtProperties jwtProperties;

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody JoinRequestDto joinRequestDto) {
        joinService.join(joinRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new HttpResponse(HttpStatus.CREATED, SuccessMessage.WRITE_SUCCESS.getMessage(), null));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        LoginResult loginResult = loginService.login(loginRequestDto);
        ResponseCookie refreshCookie = CookieUtil.createResponseCookie(loginResult.getRefreshToken(), jwtProperties.getRefreshExpirationTime());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.LOGIN_SUCCESS.getMessage(), loginResult.getLoginResponseDto()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshCookie,
                                     HttpServletResponse response) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            CookieUtil.clearRefreshCookie(response);
            throw new RefreshTokenNotExistsException();
        }

        TokenResult tokens = loginService.reissueTokens(refreshCookie);
        ResponseCookie newRefreshCookie = CookieUtil.createResponseCookie(tokens.getRefreshToken(), jwtProperties.getRefreshExpirationTime());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.CREATE_TOKENS.getMessage(), tokens.getAccessToken()));
    }

    @PostMapping("/email-verification/request")
    public ResponseEntity<?> sendEmailVerification(@RequestBody EmailVerificationSendRequestDto emailVerificationSendRequestDto) {
        joinService.sendJoinEmail(emailVerificationSendRequestDto.getEmail());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new HttpResponse(HttpStatus.CREATED, SuccessMessage.EMAIL_SEND_SUCCESS.getMessage(), null));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<?> confirmEmail(@RequestBody EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto) {
        EmailVerificationConfirmResponseDto emailVerificationResult = joinService.verifyEmailCode(emailVerificationConfirmRequestDto);

        String message;
        if (emailVerificationResult.getStatus() == EmailVerifyStatus.VERIFIED) {
            message = SuccessMessage.EMAIL_VERIFIED.getMessage();
        } else {
            message = ErrorMessage.EMAIL_VERIFICATION_FAILED.getMessage();
        }
        return ResponseEntity
                .ok()
                .body(new HttpResponse(HttpStatus.OK, message, emailVerificationResult));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDto requestDto) {
        passwordResetService.requestPasswordReset(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new HttpResponse(HttpStatus.CREATED, SuccessMessage.PASSWORD_RESET_EMAIL_SENT.getMessage(), null));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<?> verifyPasswordResetCode(@RequestBody PasswordResetVerifyRequestDto requestDto) {
        EmailVerificationConfirmResponseDto result = passwordResetService.verifyCode(requestDto);

        String message;
        if (result.getStatus() == EmailVerifyStatus.VERIFIED) {
            message = SuccessMessage.EMAIL_VERIFIED.getMessage();
        } else {
            message = ErrorMessage.EMAIL_VERIFICATION_FAILED.getMessage();
        }
        return ResponseEntity
                .ok()
                .body(new HttpResponse(HttpStatus.OK, message, result));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@RequestBody PasswordResetConfirmRequestDto requestDto) {
        passwordResetService.resetPassword(requestDto);
        return ResponseEntity
                .ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.PASSWORD_RESET_SUCCESS.getMessage(), null));
    }

}
