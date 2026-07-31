package com.keyfeed.keyfeedmonolithic.global.error.exception;

import org.springframework.http.HttpStatus;

public class InternalApiRequestException extends CustomException {

    public InternalApiRequestException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public InternalApiRequestException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE);
    }

}
