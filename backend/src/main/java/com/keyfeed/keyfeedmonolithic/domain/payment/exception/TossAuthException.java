package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class TossAuthException extends CustomException {

    public TossAuthException() {
        super(ErrorMessage.TOSS_UNAUTHORIZED.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
