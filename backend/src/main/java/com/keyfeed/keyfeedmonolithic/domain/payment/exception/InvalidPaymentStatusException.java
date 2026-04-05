package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InvalidPaymentStatusException extends CustomException {
    public InvalidPaymentStatusException() {
        super(ErrorMessage.INVALID_PAYMENT_STATUS.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
