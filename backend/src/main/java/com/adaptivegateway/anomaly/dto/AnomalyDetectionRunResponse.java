package com.adaptivegateway.anomaly.dto;

import java.time.Instant;

public record AnomalyDetectionRunResponse(
        Instant evaluatedSince,
        int routeMetricsEvaluated,
        int statisticSnapshotsCreated,
        int anomaliesCreated,
        int alertsCreated,
        int strictnessAdjustmentsCreated
) {
}
