package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class PaymentFailedException extends CustomException {

    public PaymentFailedException() {
        super(ErrorMessage.PAYMENT_FAILED.getMessage(), HttpStatus.PAYMENT_REQUIRED);
    }
}
