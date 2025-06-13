package com.tskrypko.streaming.exception;

import org.springframework.http.HttpStatus;

public class VideoAccessDeniedException extends StreamingRuntimeException {
    
    public VideoAccessDeniedException(String message) {
        super(message, ErrorCode.VIDEO_ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    public VideoAccessDeniedException(String message, Throwable cause) {
        super(message, ErrorCode.VIDEO_ACCESS_DENIED, HttpStatus.FORBIDDEN, cause);
    }
} 