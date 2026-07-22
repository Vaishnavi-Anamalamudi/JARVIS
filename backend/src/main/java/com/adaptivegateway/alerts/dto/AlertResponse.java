package com.adaptivegateway.alerts.dto;

import com.adaptivegateway.alerts.enums.AlertSeverity;
import com.adaptivegateway.alerts.enums.AlertStatus;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID anomalyRecordId,
        UUID routeId,
        String routeKey,
        String alertType,
        AlertSeverity severity,
        String title,
        String message,
        AlertStatus status,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
