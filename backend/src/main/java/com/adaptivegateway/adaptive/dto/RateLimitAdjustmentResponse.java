package com.adaptivegateway.adaptive.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RateLimitAdjustmentResponse(
        UUID id,
        UUID policyId,
        String policyName,
        UUID routeId,
        String routeKey,
        BigDecimal previousStrictnessFactor,
        BigDecimal newStrictnessFactor,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
}
