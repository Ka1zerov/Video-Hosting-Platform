package com.tskrypko.upload.exception;

import org.springframework.http.HttpStatus;

public class S3OperationException extends UploadRuntimeException {
    
    public S3OperationException(String message) {
        super(message, ErrorCode.S3_OPERATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public S3OperationException(String message, Throwable cause) {
        super(message, ErrorCode.S3_OPERATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
} 