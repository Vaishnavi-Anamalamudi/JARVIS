package com.adaptivegateway.anomaly.entity;

import com.adaptivegateway.anomaly.enums.AnomalySeverity;
import com.adaptivegateway.anomaly.enums.AnomalyStatus;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anomaly_records")
public class AnomalyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "stats_snapshot_id", nullable = false)
    private AnomalyStatSnapshot statsSnapshot;

    @Column(name = "consumer_id")
    private UUID consumerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private GatewayRoute route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AnomalySeverity severity;

    @Column(name = "metric_name", nullable = false, length = 80)
    private String metricName;

    @Column(name = "observed_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal observedValue;

    @Column(name = "threshold_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal thresholdValue;

    @Column(name = "z_score", nullable = false, precision = 18, scale = 6)
    private BigDecimal zScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AnomalyStatus status;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public AnomalyStatSnapshot getStatsSnapshot() {
        return statsSnapshot;
    }

    public void setStatsSnapshot(AnomalyStatSnapshot statsSnapshot) {
        this.statsSnapshot = statsSnapshot;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(UUID consumerId) {
        this.consumerId = consumerId;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public void setRoute(GatewayRoute route) {
        this.route = route;
    }

    public AnomalySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AnomalySeverity severity) {
        this.severity = severity;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public BigDecimal getObservedValue() {
        return observedValue;
    }

    public void setObservedValue(BigDecimal observedValue) {
        this.observedValue = observedValue;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public BigDecimal getZScore() {
        return zScore;
    }

    public void setZScore(BigDecimal zScore) {
        this.zScore = zScore;
    }

    public AnomalyStatus getStatus() {
        return status;
    }

    public void setStatus(AnomalyStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
