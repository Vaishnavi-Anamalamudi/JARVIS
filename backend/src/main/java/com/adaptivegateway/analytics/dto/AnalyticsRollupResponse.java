package com.adaptivegateway.analytics.dto;

import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnalyticsRollupResponse(
        UUID id,
        Instant bucketStart,
        Instant bucketEnd,
        AnalyticsGranularity granularity,
        UUID consumerId,
        String consumerName,
        UUID routeId,
        String routeKey,
        Long totalRequests,
        Long allowedRequests,
        Long blockedRequests,
        Long errorRequests,
        BigDecimal blockedRate,
        BigDecimal errorRate,
        BigDecimal avgResponseTimeMs,
        BigDecimal p95ResponseTimeMs,
        Integer uniqueSourceIps,
        Instant createdAt,
        Instant updatedAt
) {
}
