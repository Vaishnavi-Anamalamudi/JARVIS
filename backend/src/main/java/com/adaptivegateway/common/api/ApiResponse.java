package com.adaptivegateway.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String correlationId,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(String correlationId, String message, T data) {
        return new ApiResponse<>(true, correlationId, message, data, Instant.now());
    }
}
