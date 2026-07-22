package com.adaptivegateway.adaptive.controller;

import com.adaptivegateway.adaptive.dto.AdaptiveLearningRunResponse;
import com.adaptivegateway.adaptive.dto.RateLimitAdjustmentResponse;
import com.adaptivegateway.adaptive.dto.RouteMetricResponse;
import com.adaptivegateway.adaptive.dto.TrafficHeatmapBucketResponse;
import com.adaptivegateway.adaptive.service.AdaptiveLearningQueryService;
import com.adaptivegateway.adaptive.service.AdaptiveLearningService;
import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/adaptive-learning")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Adaptive Learning")
public class AdaptiveLearningController {

    private final AdaptiveLearningService adaptiveLearningService;
    private final AdaptiveLearningQueryService queryService;

    public AdaptiveLearningController(
            AdaptiveLearningService adaptiveLearningService,
            AdaptiveLearningQueryService queryService
    ) {
        this.adaptiveLearningService = adaptiveLearningService;
        this.queryService = queryService;
    }

    @PostMapping("/run")
    @Operation(summary = "Run adaptive learning aggregation and strictness adjustment")
    public ApiResponse<AdaptiveLearningRunResponse> runLearningCycle(
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Adaptive learning cycle completed", adaptiveLearningService.runLearningCycle());
    }

    @GetMapping("/route-metrics")
    @Operation(summary = "List route traffic metrics produced by adaptive learning")
    public ApiResponse<PageResponse<RouteMetricResponse>> listRouteMetrics(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "calculatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Route metrics read successfully",
                queryService.listRouteMetrics(page, size, sort, direction));
    }

    @GetMapping("/traffic-heatmap")
    @Operation(summary = "List route traffic heatmap buckets produced by adaptive learning")
    public ApiResponse<PageResponse<TrafficHeatmapBucketResponse>> listHeatmapBuckets(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "bucketStart") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Traffic heatmap buckets read successfully",
                queryService.listHeatmapBuckets(page, size, sort, direction));
    }

    @GetMapping("/adjustments")
    @Operation(summary = "List adaptive rate limit strictness adjustments")
    public ApiResponse<PageResponse<RateLimitAdjustmentResponse>> listAdjustments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Adaptive adjustments read successfully",
                queryService.listAdjustments(page, size, sort, direction));
    }
}
