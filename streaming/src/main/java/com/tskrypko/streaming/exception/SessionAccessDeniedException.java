package com.tskrypko.streaming.exception;

import org.springframework.http.HttpStatus;

public class SessionAccessDeniedException extends StreamingRuntimeException {
    
    public SessionAccessDeniedException(String message) {
        super(message, ErrorCode.SESSION_ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    public SessionAccessDeniedException(String message, Throwable cause) {
        super(message, ErrorCode.SESSION_ACCESS_DENIED, HttpStatus.FORBIDDEN, cause);
    }
} 