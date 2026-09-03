package com.adaptivegateway.analytics.mapper;

import com.adaptivegateway.analytics.dto.AnalyticsRollupResponse;
import com.adaptivegateway.analytics.dto.ClientMetricResponse;
import com.adaptivegateway.analytics.entity.AnalyticsRollup;
import com.adaptivegateway.analytics.entity.ClientMetric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMapper {

    public AnalyticsRollupResponse toResponse(AnalyticsRollup rollup) {
        return new AnalyticsRollupResponse(
                rollup.getId(),
                rollup.getBucketStart(),
                rollup.getBucketEnd(),
                rollup.getGranularity(),
                rollup.getConsumer() == null ? null : rollup.getConsumer().getId(),
                rollup.getConsumer() == null ? null : rollup.getConsumer().getName(),
                rollup.getRoute() == null ? null : rollup.getRoute().getId(),
                rollup.getRoute() == null ? null : rollup.getRoute().getRouteKey(),
                rollup.getTotalRequests(),
                rollup.getAllowedRequests(),
                rollup.getBlockedRequests(),
                rollup.getErrorRequests(),
                rate(rollup.getBlockedRequests(), rollup.getTotalRequests()),
                rate(rollup.getErrorRequests(), rollup.getTotalRequests()),
                rollup.getAvgResponseTimeMs(),
                rollup.getP95ResponseTimeMs(),
                rollup.getUniqueSourceIps(),
                rollup.getCreatedAt(),
                rollup.getUpdatedAt()
        );
    }

    public ClientMetricResponse toResponse(ClientMetric metric) {
        return new ClientMetricResponse(
                metric.getId(),
                metric.getConsumer().getId(),
                metric.getConsumer().getName(),
                metric.getCalculatedAt(),
                metric.getWindowSeconds(),
                metric.getRequestsPerSecond(),
                metric.getBlockedRate(),
                metric.getErrorRate(),
                metric.getAvgLatencyMs(),
                metric.getCreatedAt(),
                metric.getUpdatedAt()
        );
    }

    private BigDecimal rate(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }
}
