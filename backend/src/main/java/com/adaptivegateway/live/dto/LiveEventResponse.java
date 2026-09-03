package com.adaptivegateway.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveEventResponse(
        UUID id,
        String type,
        String resourceType,
        UUID resourceId,
        UUID correlationId,
        String message,
        Map<String, Object> payload,
        Instant createdAt
) {
}
