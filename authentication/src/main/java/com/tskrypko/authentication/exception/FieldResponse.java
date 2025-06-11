package com.tskrypko.authentication.exception;

public record FieldResponse(
        String field,
        String error
) {
}

