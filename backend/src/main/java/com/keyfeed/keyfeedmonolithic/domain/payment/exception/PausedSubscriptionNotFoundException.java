package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class PausedSubscriptionNotFoundException extends CustomException {
    public PausedSubscriptionNotFoundException() {
        super(ErrorMessage.PAUSED_SUBSCRIPTION_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND);
    }
}
