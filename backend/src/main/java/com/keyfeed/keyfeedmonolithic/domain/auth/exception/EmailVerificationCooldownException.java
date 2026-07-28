package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationCooldownException extends CustomException {

    public EmailVerificationCooldownException() {
        super(ErrorMessage.EMAIL_VERIFICATION_COOLDOWN.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

}
