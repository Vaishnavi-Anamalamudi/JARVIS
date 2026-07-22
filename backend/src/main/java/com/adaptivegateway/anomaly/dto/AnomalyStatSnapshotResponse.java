package com.adaptivegateway.anomaly.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnomalyStatSnapshotResponse(
        UUID id,
        UUID routeId,
        String routeKey,
        String metricName,
        Integer windowSeconds,
        Integer sampleCount,
        BigDecimal rollingMean,
        BigDecimal rollingVariance,
        BigDecimal rollingStddev,
        BigDecimal rollingZScore,
        BigDecimal emaValue,
        BigDecimal adaptiveThreshold,
        Instant calculatedAt
) {
}
