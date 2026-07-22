package com.adaptivegateway.anomaly.controller;

import com.adaptivegateway.anomaly.dto.AnomalyDetectionRunResponse;
import com.adaptivegateway.anomaly.dto.AnomalyRecordResponse;
import com.adaptivegateway.anomaly.dto.AnomalyStatSnapshotResponse;
import com.adaptivegateway.anomaly.enums.AnomalyStatus;
import com.adaptivegateway.anomaly.service.AnomalyDetectionService;
import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/anomalies")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Anomaly Detection")
public class AnomalyDetectionController {

    private final AnomalyDetectionService anomalyDetectionService;

    public AnomalyDetectionController(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @PostMapping("/run")
    @Operation(summary = "Run anomaly detection over persisted route metrics")
    public ApiResponse<AnomalyDetectionRunResponse> run(
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Anomaly detection completed", anomalyDetectionService.runDetectionCycle());
    }

    @GetMapping
    @Operation(summary = "List anomaly records")
    public ApiResponse<PageResponse<AnomalyRecordResponse>> anomalies(
            @RequestParam(required = false) AnomalyStatus status,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        var response = anomalyDetectionService.listAnomalies(status, routeId, page, size, sort, direction);
        return ApiResponse.success(correlationId, "Anomaly records read successfully", response);
    }

    @GetMapping("/snapshots")
    @Operation(summary = "List rolling statistic snapshots")
    public ApiResponse<PageResponse<AnomalyStatSnapshotResponse>> snapshots(
            @RequestParam(required = false) UUID routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "calculatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        var response = anomalyDetectionService.listSnapshots(routeId, page, size, sort, direction);
        return ApiResponse.success(correlationId, "Anomaly statistic snapshots read successfully", response);
    }
}
