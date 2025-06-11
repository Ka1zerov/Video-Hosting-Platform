package com.tskrypko.upload.exception;

import org.springframework.http.HttpStatus;

public class UserIdHeaderNotFoundException extends UploadRuntimeException {
    
    public UserIdHeaderNotFoundException() {
        super("X-User-Id header not found in request", ErrorCode.USER_ID_HEADER_NOT_FOUND, HttpStatus.UNAUTHORIZED);
    }
    
    public UserIdHeaderNotFoundException(String message) {
        super(message, ErrorCode.USER_ID_HEADER_NOT_FOUND, HttpStatus.UNAUTHORIZED);
    }
} 