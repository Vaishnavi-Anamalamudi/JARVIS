package com.adaptivegateway.anomaly.dto;

import com.adaptivegateway.anomaly.enums.AnomalySeverity;
import com.adaptivegateway.anomaly.enums.AnomalyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnomalyRecordResponse(
        UUID id,
        UUID statsSnapshotId,
        UUID routeId,
        String routeKey,
        AnomalySeverity severity,
        String metricName,
        BigDecimal observedValue,
        BigDecimal thresholdValue,
        BigDecimal zScore,
        AnomalyStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
