package com.adaptivegateway.adaptive.mapper;

import com.adaptivegateway.adaptive.dto.RateLimitAdjustmentResponse;
import com.adaptivegateway.adaptive.dto.RouteMetricResponse;
import com.adaptivegateway.adaptive.dto.TrafficHeatmapBucketResponse;
import com.adaptivegateway.adaptive.entity.RateLimitAdjustment;
import com.adaptivegateway.adaptive.entity.RouteMetric;
import com.adaptivegateway.adaptive.entity.TrafficHeatmapBucket;
import org.springframework.stereotype.Component;

@Component
public class AdaptiveLearningMapper {

    public RouteMetricResponse toResponse(RouteMetric metric) {
        return new RouteMetricResponse(
                metric.getId(),
                metric.getRoute().getId(),
                metric.getRoute().getRouteKey(),
                metric.getRoute().getName(),
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

    public TrafficHeatmapBucketResponse toResponse(TrafficHeatmapBucket bucket) {
        return new TrafficHeatmapBucketResponse(
                bucket.getId(),
                bucket.getBucketStart(),
                bucket.getDayOfWeek(),
                bucket.getHourOfDay(),
                bucket.getRoute().getId(),
                bucket.getRoute().getRouteKey(),
                bucket.getRequestCount(),
                bucket.getBlockedCount(),
                bucket.getAvgLatencyMs(),
                bucket.getCreatedAt(),
                bucket.getUpdatedAt()
        );
    }

    public RateLimitAdjustmentResponse toResponse(RateLimitAdjustment adjustment) {
        return new RateLimitAdjustmentResponse(
                adjustment.getId(),
                adjustment.getPolicy().getId(),
                adjustment.getPolicy().getName(),
                adjustment.getRoute().getId(),
                adjustment.getRoute().getRouteKey(),
                adjustment.getPreviousStrictnessFactor(),
                adjustment.getNewStrictnessFactor(),
                adjustment.getReason(),
                adjustment.getCreatedAt(),
                adjustment.getUpdatedAt()
        );
    }
}
