package com.keyfeed.keyfeedmonolithic.global.error.exception;

import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InternalServerProcessingException extends CustomException {

    public InternalServerProcessingException() {
        super(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerProcessingException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
