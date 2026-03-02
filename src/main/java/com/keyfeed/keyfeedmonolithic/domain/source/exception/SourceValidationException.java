package com.keyfeed.keyfeedmonolithic.domain.source.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SourceValidationException extends CustomException {

    public SourceValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}