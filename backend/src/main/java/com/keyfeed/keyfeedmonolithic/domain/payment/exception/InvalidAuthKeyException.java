package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InvalidAuthKeyException extends CustomException {

    public InvalidAuthKeyException() {
        super(ErrorMessage.INVALID_AUTH_KEY.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
