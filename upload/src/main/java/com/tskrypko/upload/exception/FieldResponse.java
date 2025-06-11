package com.tskrypko.upload.exception;

public record FieldResponse(
        String field,
        String error
) {
} 