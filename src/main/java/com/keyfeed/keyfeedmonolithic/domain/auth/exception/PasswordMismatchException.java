package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends CustomException {

    public PasswordMismatchException() {
        super(ErrorMessage.PASSWORD_MISMATCH.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
