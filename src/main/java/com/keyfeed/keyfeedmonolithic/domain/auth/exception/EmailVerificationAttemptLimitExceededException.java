package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationAttemptLimitExceededException extends CustomException {

    public EmailVerificationAttemptLimitExceededException() {
        super(ErrorMessage.EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

}
