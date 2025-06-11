package com.tskrypko.authentication.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserNotAuthenticatedException extends AuthenticationRuntimeException {

    public UserNotAuthenticatedException() {
        super("User is not authenticated",
                ErrorCode.USER_NOT_AUTHENTICATED,
                HttpStatus.UNAUTHORIZED
        );
    }
}
