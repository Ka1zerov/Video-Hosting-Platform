package com.tskrypko.streaming.exception;

import org.springframework.http.HttpStatus;

public class SessionGenerationException extends StreamingRuntimeException {
    
    public SessionGenerationException(String message) {
        super(message, ErrorCode.SESSION_GENERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public SessionGenerationException(String message, Throwable cause) {
        super(message, ErrorCode.SESSION_GENERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
} 