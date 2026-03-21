package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationExpiredException extends CustomException {

    public EmailVerificationExpiredException() {
        super(ErrorMessage.EMAIL_VERIFICATION_EXPIRED.getMessage(), HttpStatus.GONE);
    }

}
