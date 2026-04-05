package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class RefundPeriodExpiredException extends CustomException {
    public RefundPeriodExpiredException() {
        super(ErrorMessage.REFUND_PERIOD_EXPIRED.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
