package com.keyfeed.keyfeedmonolithic.domain.bookmark.exception;

import com.keyfeed.keyfeedmonolithic.global.error.exception.CustomException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class FolderLimitExceededException extends CustomException {
    public FolderLimitExceededException() {
        super(ErrorMessage.BOOKMARK_FOLDER_LIMIT_EXCEEDED.getMessage(), HttpStatus.BAD_REQUEST);
    }
}