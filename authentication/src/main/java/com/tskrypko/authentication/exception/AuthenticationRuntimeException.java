package com.tskrypko.authentication.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthenticationRuntimeException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    public AuthenticationRuntimeException(String message, ErrorCode code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public AuthenticationRuntimeException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_SERVER_ERROR;
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
