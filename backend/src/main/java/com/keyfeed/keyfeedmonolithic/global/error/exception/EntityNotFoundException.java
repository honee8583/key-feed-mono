package com.keyfeed.keyfeedmonolithic.global.error.exception;

import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EntityNotFoundException extends CustomException {

    public EntityNotFoundException(String entity, Object data) {
        super(ErrorMessage.ENTITY_NOT_FOUND.getMessage() + entity + ": " + data, HttpStatus.CONFLICT);
    }

}
