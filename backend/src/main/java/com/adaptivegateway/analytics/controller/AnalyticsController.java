package com.adaptivegateway.analytics.controller;

import com.adaptivegateway.analytics.dto.AnalyticsRollupResponse;
import com.adaptivegateway.analytics.dto.AnalyticsRunResponse;
import com.adaptivegateway.analytics.dto.AnalyticsSummaryResponse;
import com.adaptivegateway.analytics.dto.ClientMetricResponse;
import com.adaptivegateway.analytics.dto.ClientMetricsRunResponse;
import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import com.adaptivegateway.analytics.service.AnalyticsService;
import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/analytics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Read analytics summary for recent gateway traffic")
    public ApiResponse<AnalyticsSummaryResponse> summary(
            @RequestParam(defaultValue = "3600") @Min(60) @Max(2_592_000) int windowSeconds,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Analytics summary read successfully", analyticsService.summary(windowSeconds));
    }

    @PostMapping("/rollups/run")
    @Operation(summary = "Generate analytics rollups from request history")
    public ApiResponse<AnalyticsRunResponse> runRollups(
            @RequestParam(defaultValue = "HOUR") AnalyticsGranularity granularity,
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int lookbackBuckets,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Analytics rollups generated", analyticsService.runRollups(granularity, lookbackBuckets));
    }

    @GetMapping("/rollups")
    @Operation(summary = "List generated analytics rollups")
    public ApiResponse<PageResponse<AnalyticsRollupResponse>> rollups(
            @RequestParam(required = false) AnalyticsGranularity granularity,
            @RequestParam(required = false) UUID consumerId,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "bucketStart") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Analytics rollups read successfully",
                analyticsService.listRollups(granularity, consumerId, routeId, page, size, sort, direction));
    }

    @PostMapping("/client-metrics/run")
    @Operation(summary = "Generate client metrics from recent request history")
    public ApiResponse<ClientMetricsRunResponse> runClientMetrics(
            @RequestParam(defaultValue = "300") @Min(60) @Max(86_400) int windowSeconds,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Client metrics generated", analyticsService.runClientMetrics(windowSeconds));
    }

    @GetMapping("/client-metrics")
    @Operation(summary = "List generated client metrics")
    public ApiResponse<PageResponse<ClientMetricResponse>> clientMetrics(
            @RequestParam(required = false) UUID consumerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "calculatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Client metrics read successfully",
                analyticsService.listClientMetrics(consumerId, page, size, sort, direction));
    }
}
