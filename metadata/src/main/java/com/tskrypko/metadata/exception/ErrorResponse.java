package com.tskrypko.metadata.exception;

import java.util.List;

public record ErrorResponse(
        String message,
        ErrorCode code,
        List<FieldResponse> fields
) {
}
