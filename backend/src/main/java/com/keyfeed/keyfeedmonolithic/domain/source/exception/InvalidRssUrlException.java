package com.keyfeed.keyfeedmonolithic.domain.source.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import org.springframework.http.HttpStatus;

import static com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage.INVALID_RSS_URL;

public class InvalidRssUrlException extends CustomException {

    public InvalidRssUrlException() {
        super(INVALID_RSS_URL.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
