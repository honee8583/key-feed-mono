package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InvalidPaymentMethodException extends CustomException {

    public InvalidPaymentMethodException() {
        super(ErrorMessage.INVALID_PAYMENT_METHOD.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
