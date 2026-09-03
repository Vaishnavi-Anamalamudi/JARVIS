package com.adaptivegateway.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsSummaryResponse(
        Instant windowStart,
        Instant windowEnd,
        Integer windowSeconds,
        Long totalRequests,
        Long allowedRequests,
        Long blockedRequests,
        Long errorRequests,
        BigDecimal blockedRate,
        BigDecimal errorRate,
        BigDecimal avgResponseTimeMs,
        BigDecimal p95ResponseTimeMs,
        Integer uniqueSourceIps
) {
}
