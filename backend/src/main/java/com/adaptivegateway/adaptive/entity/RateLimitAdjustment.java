package com.adaptivegateway.adaptive.entity;

import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
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
@Table(name = "rate_limit_adjustments")
public class RateLimitAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private RateLimitPolicy policy;

    @Column(name = "anomaly_record_id")
    private UUID anomalyRecordId;

    @Column(name = "consumer_id")
    private UUID consumerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private GatewayRoute route;

    @Column(name = "previous_strictness_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal previousStrictnessFactor;

    @Column(name = "new_strictness_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal newStrictnessFactor;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public UUID getAnomalyRecordId() {
        return anomalyRecordId;
    }

    public void setAnomalyRecordId(UUID anomalyRecordId) {
        this.anomalyRecordId = anomalyRecordId;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(UUID consumerId) {
        this.consumerId = consumerId;
    }

    public RateLimitPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(RateLimitPolicy policy) {
        this.policy = policy;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public void setRoute(GatewayRoute route) {
        this.route = route;
    }

    public BigDecimal getPreviousStrictnessFactor() {
        return previousStrictnessFactor;
    }

    public void setPreviousStrictnessFactor(BigDecimal previousStrictnessFactor) {
        this.previousStrictnessFactor = previousStrictnessFactor;
    }

    public BigDecimal getNewStrictnessFactor() {
        return newStrictnessFactor;
    }

    public void setNewStrictnessFactor(BigDecimal newStrictnessFactor) {
        this.newStrictnessFactor = newStrictnessFactor;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
