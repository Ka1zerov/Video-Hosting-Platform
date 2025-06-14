package com.tskrypko.metadata.exception;

public record FieldResponse(
        String field,
        String error
) {
}
