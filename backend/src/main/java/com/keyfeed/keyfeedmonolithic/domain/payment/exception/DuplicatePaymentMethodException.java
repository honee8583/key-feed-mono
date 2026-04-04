package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class DuplicatePaymentMethodException extends CustomException {

    public DuplicatePaymentMethodException() {
        super(ErrorMessage.DUPLICATE_PAYMENT_METHOD.getMessage(), HttpStatus.CONFLICT);
    }
}
