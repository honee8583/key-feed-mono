package com.keyfeed.keyfeedmonolithic.domain.keyword.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class KeywordLimitExceededException extends CustomException {

    public KeywordLimitExceededException() {
        super(ErrorMessage.KEYWORD_LIMIT_EXCEEDED.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
