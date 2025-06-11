package com.tskrypko.upload.exception;

public enum ErrorCode {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    REQUEST_VALIDATION_ERROR("REQUEST_VALIDATION_ERROR"),
    USER_ID_HEADER_NOT_FOUND("USER_ID_HEADER_NOT_FOUND"),
    FILE_UPLOAD_ERROR("FILE_UPLOAD_ERROR"),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE"),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED"),
    S3_OPERATION_ERROR("S3_OPERATION_ERROR"),
    VIDEO_NOT_FOUND("VIDEO_NOT_FOUND"),
    ACCESS_DENIED("ACCESS_DENIED");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
} 