package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationAlreadyDoneException extends CustomException {

    public EmailVerificationAlreadyDoneException() {
        super(ErrorMessage.EMAIL_ALREADY_VERIFIED.getMessage(), HttpStatus.CONFLICT);
    }

}
