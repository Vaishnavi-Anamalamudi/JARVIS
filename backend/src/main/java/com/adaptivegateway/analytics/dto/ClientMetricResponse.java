package com.adaptivegateway.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClientMetricResponse(
        UUID id,
        UUID consumerId,
        String consumerName,
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
