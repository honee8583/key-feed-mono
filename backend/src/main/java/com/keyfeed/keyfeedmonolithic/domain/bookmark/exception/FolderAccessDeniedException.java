package com.keyfeed.keyfeedmonolithic.domain.bookmark.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class FolderAccessDeniedException extends CustomException {

    public FolderAccessDeniedException() {
        super(ErrorMessage.FORBIDDEN.getMessage(), HttpStatus.FORBIDDEN);
    }
}