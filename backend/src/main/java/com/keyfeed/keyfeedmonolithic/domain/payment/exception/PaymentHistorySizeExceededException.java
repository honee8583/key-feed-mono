package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class PaymentHistorySizeExceededException extends CustomException {
    public PaymentHistorySizeExceededException() {
        super(ErrorMessage.PAYMENT_HISTORY_SIZE_EXCEEDED.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
