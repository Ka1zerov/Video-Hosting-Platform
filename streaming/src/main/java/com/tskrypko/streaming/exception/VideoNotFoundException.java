package com.tskrypko.streaming.exception;

import org.springframework.http.HttpStatus;

public class VideoNotFoundException extends StreamingRuntimeException {
    
    public VideoNotFoundException(String message) {
        super(message, ErrorCode.VIDEO_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public VideoNotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.VIDEO_NOT_FOUND, HttpStatus.NOT_FOUND, cause);
    }
} 