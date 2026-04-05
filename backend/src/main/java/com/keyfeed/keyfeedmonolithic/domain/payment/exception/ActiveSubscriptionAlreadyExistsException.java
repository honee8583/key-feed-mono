package com.keyfeed.keyfeedmonolithic.domain.payment.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class ActiveSubscriptionAlreadyExistsException extends CustomException {
    public ActiveSubscriptionAlreadyExistsException() {
        super(ErrorMessage.ACTIVE_SUBSCRIPTION_ALREADY_EXISTS.getMessage(), HttpStatus.CONFLICT);
    }
}
