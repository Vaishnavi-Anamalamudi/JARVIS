package com.adaptivegateway.anomaly.mapper;

import com.adaptivegateway.anomaly.dto.AnomalyRecordResponse;
import com.adaptivegateway.anomaly.dto.AnomalyStatSnapshotResponse;
import com.adaptivegateway.anomaly.entity.AnomalyRecord;
import com.adaptivegateway.anomaly.entity.AnomalyStatSnapshot;
import org.springframework.stereotype.Component;

@Component
public class AnomalyDetectionMapper {

    public AnomalyStatSnapshotResponse toResponse(AnomalyStatSnapshot snapshot) {
        return new AnomalyStatSnapshotResponse(
                snapshot.getId(),
                snapshot.getRoute().getId(),
                snapshot.getRoute().getRouteKey(),
                snapshot.getMetricName(),
                snapshot.getWindowSeconds(),
                snapshot.getSampleCount(),
                snapshot.getRollingMean(),
                snapshot.getRollingVariance(),
                snapshot.getRollingStddev(),
                snapshot.getRollingZScore(),
                snapshot.getEmaValue(),
                snapshot.getAdaptiveThreshold(),
                snapshot.getCalculatedAt()
        );
    }

    public AnomalyRecordResponse toResponse(AnomalyRecord record) {
        return new AnomalyRecordResponse(
                record.getId(),
                record.getStatsSnapshot().getId(),
                record.getRoute().getId(),
                record.getRoute().getRouteKey(),
                record.getSeverity(),
                record.getMetricName(),
                record.getObservedValue(),
                record.getThresholdValue(),
                record.getZScore(),
                record.getStatus(),
                record.getDescription(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
