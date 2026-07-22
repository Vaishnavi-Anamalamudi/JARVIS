package com.adaptivegateway.ratelimit.entity;

import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitDecisionValue;
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
@Table(name = "rate_limit_decisions")
public class RateLimitDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "request_log_id", nullable = false)
    private RequestLog requestLog;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private RateLimitPolicy policy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment_id")
    private RateLimitAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateLimitAlgorithm algorithm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateLimitDecisionValue decision;

    @Column(name = "effective_limit", nullable = false)
    private Integer effectiveLimit;

    @Column(name = "observed_count")
    private Integer observedCount;

    @Column(name = "remaining_tokens")
    private Integer remainingTokens;

    @Column(name = "retry_after_seconds")
    private Integer retryAfterSeconds;

    @Column(name = "strictness_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal strictnessFactor;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public RequestLog getRequestLog() {
        return requestLog;
    }

    public RateLimitPolicy getPolicy() {
        return policy;
    }

    public RateLimitAssignment getAssignment() {
        return assignment;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public RateLimitDecisionValue getDecision() {
        return decision;
    }

    public Integer getEffectiveLimit() {
        return effectiveLimit;
    }

    public Integer getObservedCount() {
        return observedCount;
    }

    public Integer getRemainingTokens() {
        return remainingTokens;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public BigDecimal getStrictnessFactor() {
        return strictnessFactor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setRequestLog(RequestLog requestLog) {
        this.requestLog = requestLog;
    }

    public void setPolicy(RateLimitPolicy policy) {
        this.policy = policy;
    }

    public void setAssignment(RateLimitAssignment assignment) {
        this.assignment = assignment;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void setDecision(RateLimitDecisionValue decision) {
        this.decision = decision;
    }

    public void setEffectiveLimit(Integer effectiveLimit) {
        this.effectiveLimit = effectiveLimit;
    }

    public void setObservedCount(Integer observedCount) {
        this.observedCount = observedCount;
    }

    public void setRemainingTokens(Integer remainingTokens) {
        this.remainingTokens = remainingTokens;
    }

    public void setRetryAfterSeconds(Integer retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public void setStrictnessFactor(BigDecimal strictnessFactor) {
        this.strictnessFactor = strictnessFactor;
    }
}
