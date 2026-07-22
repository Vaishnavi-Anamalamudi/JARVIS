package com.adaptivegateway.adaptive.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RouteMetricResponse(
        UUID id,
        UUID routeId,
        String routeKey,
        String routeName,
        Instant calculatedAt,
        Integer windowSeconds,
        BigDecimal requestsPerSecond,
        BigDecimal blockedRate,
        BigDecimal errorRate,
        BigDecimal avgLatencyMs,
        Instant createdAt,
        Instant updatedAt
) {
}
