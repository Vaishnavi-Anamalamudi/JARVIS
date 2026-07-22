package com.adaptivegateway.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        boolean success,
        String correlationId,
        String errorCode,
        String message,
        String path,
        List<ErrorField> fields,
        Instant timestamp
) {

    public static ErrorResponse of(
            String correlationId,
            String errorCode,
            String message,
            String path,
            List<ErrorField> fields
    ) {
        return new ErrorResponse(false, correlationId, errorCode, message, path, fields, Instant.now());
    }
}
