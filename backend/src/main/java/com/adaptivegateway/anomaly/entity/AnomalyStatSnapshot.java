package com.adaptivegateway.anomaly.entity;

import com.adaptivegateway.gateway.entity.GatewayRoute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "anomaly_stat_snapshots")
public class AnomalyStatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "consumer_id")
    private UUID consumerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private GatewayRoute route;

    @Column(name = "metric_name", nullable = false, length = 80)
    private String metricName;

    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Column(name = "rolling_mean", nullable = false, precision = 18, scale = 6)
    private BigDecimal rollingMean;

    @Column(name = "rolling_variance", nullable = false, precision = 18, scale = 6)
    private BigDecimal rollingVariance;

    @Column(name = "rolling_stddev", nullable = false, precision = 18, scale = 6)
    private BigDecimal rollingStddev;

    @Column(name = "rolling_z_score", nullable = false, precision = 18, scale = 6)
    private BigDecimal rollingZScore;

    @Column(name = "ema_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal emaValue;

    @Column(name = "adaptive_threshold", nullable = false, precision = 18, scale = 6)
    private BigDecimal adaptiveThreshold;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
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

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public BigDecimal getRollingMean() {
        return rollingMean;
    }

    public void setRollingMean(BigDecimal rollingMean) {
        this.rollingMean = rollingMean;
    }

    public BigDecimal getRollingVariance() {
        return rollingVariance;
    }

    public void setRollingVariance(BigDecimal rollingVariance) {
        this.rollingVariance = rollingVariance;
    }

    public BigDecimal getRollingStddev() {
        return rollingStddev;
    }

    public void setRollingStddev(BigDecimal rollingStddev) {
        this.rollingStddev = rollingStddev;
    }

    public BigDecimal getRollingZScore() {
        return rollingZScore;
    }

    public void setRollingZScore(BigDecimal rollingZScore) {
        this.rollingZScore = rollingZScore;
    }

    public BigDecimal getEmaValue() {
        return emaValue;
    }

    public void setEmaValue(BigDecimal emaValue) {
        this.emaValue = emaValue;
    }

    public BigDecimal getAdaptiveThreshold() {
        return adaptiveThreshold;
    }

    public void setAdaptiveThreshold(BigDecimal adaptiveThreshold) {
        this.adaptiveThreshold = adaptiveThreshold;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
