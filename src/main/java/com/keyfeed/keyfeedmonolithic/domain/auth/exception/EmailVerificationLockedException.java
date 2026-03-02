package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationLockedException extends CustomException {

    public EmailVerificationLockedException() {
        super(ErrorMessage.EMAIL_VERIFICATION_LOCKED.getMessage(), HttpStatus.LOCKED);
    }

}
