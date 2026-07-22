package com.adaptivegateway.adaptive.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrafficHeatmapBucketResponse(
        UUID id,
        Instant bucketStart,
        Short dayOfWeek,
        Short hourOfDay,
        UUID routeId,
        String routeKey,
        Long requestCount,
        Long blockedCount,
        BigDecimal avgLatencyMs,
        Instant createdAt,
        Instant updatedAt
) {
}
