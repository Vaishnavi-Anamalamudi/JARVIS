package com.adaptivegateway.ratelimit.dto;

import com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus;
import java.time.Instant;
import java.util.UUID;

public record RateLimitAssignmentResponse(
        UUID id,
        UUID policyId,
        String policyName,
        UUID routeId,
        String routeKey,
        String routeName,
        Integer priority,
        RateLimitAssignmentStatus status,
        Instant validFrom,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt
) {
}
