package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailSendFailedException extends CustomException {

    public EmailSendFailedException() {
        super(ErrorMessage.EMAIL_SEND_FAILED.getMessage(), HttpStatus.BAD_GATEWAY);
    }

}
