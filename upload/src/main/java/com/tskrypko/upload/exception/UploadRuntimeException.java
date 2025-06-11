package com.tskrypko.upload.exception;

import org.springframework.http.HttpStatus;

public class UploadRuntimeException extends RuntimeException {
    
    private final ErrorCode code;
    private final HttpStatus status;

    public UploadRuntimeException(String message, ErrorCode code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public UploadRuntimeException(String message, ErrorCode code, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
} 