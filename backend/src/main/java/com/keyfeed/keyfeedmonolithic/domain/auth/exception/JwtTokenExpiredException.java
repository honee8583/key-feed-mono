package com.keyfeed.keyfeedmonolithic.domain.auth.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class JwtTokenExpiredException extends CustomException {

    public JwtTokenExpiredException() {
        super(ErrorMessage.TOKEN_EXPIRED.getMessage(), HttpStatus.UNAUTHORIZED);
    }

}
