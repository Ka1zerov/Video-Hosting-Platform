package com.tskrypko.streaming.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StreamingRuntimeException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public StreamingRuntimeException(String message, ErrorCode code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public StreamingRuntimeException(String message, ErrorCode code, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public StreamingRuntimeException(ErrorCode code, HttpStatus status) {
        super(code.getValue());
        this.code = code;
        this.status = status;
    }

    public StreamingRuntimeException(ErrorCode code, HttpStatus status, Throwable cause) {
        super(code.getValue(), cause);
        this.code = code;
        this.status = status;
    }
} 