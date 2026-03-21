package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class SamePasswordException extends CustomException {

    public SamePasswordException() {
        super(ErrorMessage.SAME_PASSWORD.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
