package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class PaymentMethodInUseException extends CustomException {

    public PaymentMethodInUseException() {
        super(ErrorMessage.PAYMENT_METHOD_IN_USE.getMessage(), HttpStatus.CONFLICT);
    }
}
