package com.tskrypko.streaming.exception;

public enum ErrorCode {
    // Video related errors
    VIDEO_NOT_FOUND("VIDEO_NOT_FOUND"),
    VIDEO_NOT_READY("VIDEO_NOT_READY"),
    VIDEO_ACCESS_DENIED("VIDEO_ACCESS_DENIED"),
    NO_ENCODED_QUALITIES("NO_ENCODED_QUALITIES"),
    
    // Session related errors
    SESSION_GENERATION_FAILED("SESSION_GENERATION_FAILED"),
    SESSION_ACCESS_DENIED("SESSION_ACCESS_DENIED"),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND"),
    
    // Authentication errors
    USER_ID_HEADER_MISSING("USER_ID_HEADER_MISSING"),
    AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED"),
    
    // General validation errors
    REQUEST_VALIDATION_ERROR("REQUEST_VALIDATION_ERROR"),
    INVALID_PARAMETER("INVALID_PARAMETER"),
    
    // Server errors
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 