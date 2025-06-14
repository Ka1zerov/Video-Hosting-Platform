package com.tskrypko.metadata.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    REQUEST_VALIDATION_ERROR("REQUEST_VALIDATION_ERROR"),
    VIDEO_NOT_FOUND("VIDEO_NOT_FOUND"),
    INVALID_PAGINATION("INVALID_PAGINATION"),
    SEARCH_ERROR("SEARCH_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }
}
