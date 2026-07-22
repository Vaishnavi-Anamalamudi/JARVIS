package com.adaptivegateway.adaptive.entity;

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
@Table(name = "route_metrics")
public class RouteMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private GatewayRoute route;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;

    @Column(name = "requests_per_second", nullable = false, precision = 14, scale = 6)
    private BigDecimal requestsPerSecond;

    @Column(name = "blocked_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal blockedRate;

    @Column(name = "error_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal errorRate;

    @Column(name = "avg_latency_ms", precision = 12, scale = 4)
    private BigDecimal avgLatencyMs;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public void setRoute(GatewayRoute route) {
        this.route = route;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public BigDecimal getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(BigDecimal requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public BigDecimal getBlockedRate() {
        return blockedRate;
    }

    public void setBlockedRate(BigDecimal blockedRate) {
        this.blockedRate = blockedRate;
    }

    public BigDecimal getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(BigDecimal errorRate) {
        this.errorRate = errorRate;
    }

    public BigDecimal getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(BigDecimal avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
