package com.keyfeed.keyfeedmonolithic.global.error.exception;

import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EntityAlreadyExistsException extends CustomException {

    public EntityAlreadyExistsException(String entity, Object data) {
        super(ErrorMessage.ENTITY_ALREADY_EXISTS.getMessage() + entity + ": " + data, HttpStatus.BAD_REQUEST);
    }

}
