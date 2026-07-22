package com.adaptivegateway.gateway.dto;

import com.adaptivegateway.gateway.enums.UpstreamServiceStatus;
import java.time.Instant;
import java.util.UUID;

public record UpstreamServiceResponse(
        UUID id,
        String name,
        String baseUrl,
        String healthCheckPath,
        UpstreamServiceStatus status,
        Integer timeoutMs,
        Integer retryCount,
        Instant createdAt,
        Instant updatedAt
) {
}
