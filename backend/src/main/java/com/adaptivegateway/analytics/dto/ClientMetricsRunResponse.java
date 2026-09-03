package com.adaptivegateway.analytics.dto;

import java.time.Instant;

public record ClientMetricsRunResponse(
        Instant windowStart,
        Instant windowEnd,
        Integer windowSeconds,
        Integer metricsWritten,
        Instant completedAt
) {
}
