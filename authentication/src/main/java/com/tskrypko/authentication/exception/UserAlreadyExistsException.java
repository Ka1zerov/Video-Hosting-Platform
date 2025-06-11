package com.tskrypko.authentication.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends AuthenticationRuntimeException {

    public UserAlreadyExistsException() {
        super("User already exists", ErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }
}
