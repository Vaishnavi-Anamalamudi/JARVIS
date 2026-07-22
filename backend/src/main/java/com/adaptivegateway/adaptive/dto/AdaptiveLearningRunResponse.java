package com.adaptivegateway.adaptive.dto;

import java.time.Instant;

public record AdaptiveLearningRunResponse(
        Instant windowStart,
        Instant windowEnd,
        int windowSeconds,
        int routeMetricsWritten,
        int heatmapBucketsWritten,
        int strictnessAdjustmentsWritten,
        Instant completedAt
) {
}
