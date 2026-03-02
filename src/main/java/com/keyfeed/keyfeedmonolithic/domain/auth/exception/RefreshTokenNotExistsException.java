package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class RefreshTokenNotExistsException extends CustomException {

    public RefreshTokenNotExistsException() {
        super(ErrorMessage.EMPTY_TOKEN.getMessage(), HttpStatus.UNAUTHORIZED);
    }

}
