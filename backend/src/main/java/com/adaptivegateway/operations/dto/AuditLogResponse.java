package com.adaptivegateway.operations.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorUsername,
        String action,
        String resourceType,
        UUID resourceId,
        UUID correlationId,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
