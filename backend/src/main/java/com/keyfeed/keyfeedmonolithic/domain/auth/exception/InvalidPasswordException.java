package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends CustomException {

    public InvalidPasswordException() {
        super(ErrorMessage.INVALID_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
    }

}
