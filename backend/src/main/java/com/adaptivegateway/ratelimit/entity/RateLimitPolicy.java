package com.adaptivegateway.ratelimit.entity;

import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_policies")
public class RateLimitPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateLimitAlgorithm algorithm;

    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;

    @Column(name = "max_requests", nullable = false)
    private Integer maxRequests;

    @Column(name = "bucket_capacity")
    private Integer bucketCapacity;

    @Column(name = "refill_tokens")
    private Integer refillTokens;

    @Column(name = "refill_period_seconds")
    private Integer refillPeriodSeconds;

    @Column(name = "strictness_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal strictnessFactor;

    @Column(name = "adaptive_enabled", nullable = false)
    private Boolean adaptiveEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateLimitPolicyStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public Integer getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(Integer maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Integer getBucketCapacity() {
        return bucketCapacity;
    }

    public void setBucketCapacity(Integer bucketCapacity) {
        this.bucketCapacity = bucketCapacity;
    }

    public Integer getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(Integer refillTokens) {
        this.refillTokens = refillTokens;
    }

    public Integer getRefillPeriodSeconds() {
        return refillPeriodSeconds;
    }

    public void setRefillPeriodSeconds(Integer refillPeriodSeconds) {
        this.refillPeriodSeconds = refillPeriodSeconds;
    }

    public BigDecimal getStrictnessFactor() {
        return strictnessFactor;
    }

    public void setStrictnessFactor(BigDecimal strictnessFactor) {
        this.strictnessFactor = strictnessFactor;
    }

    public Boolean getAdaptiveEnabled() {
        return adaptiveEnabled;
    }

    public void setAdaptiveEnabled(Boolean adaptiveEnabled) {
        this.adaptiveEnabled = adaptiveEnabled;
    }

    public RateLimitPolicyStatus getStatus() {
        return status;
    }

    public void setStatus(RateLimitPolicyStatus status) {
        this.status = status;
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

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
