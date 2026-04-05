package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends CustomException {
    public SubscriptionNotFoundException() {
        super(ErrorMessage.SUBSCRIPTION_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND);
    }
}
