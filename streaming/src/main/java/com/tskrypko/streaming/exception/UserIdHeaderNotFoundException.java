package com.tskrypko.streaming.exception;

public class UserIdHeaderNotFoundException extends RuntimeException {
    public UserIdHeaderNotFoundException() {
        super("X-User-Id header not found in request");
    }

    public UserIdHeaderNotFoundException(String message) {
        super(message);
    }
} 