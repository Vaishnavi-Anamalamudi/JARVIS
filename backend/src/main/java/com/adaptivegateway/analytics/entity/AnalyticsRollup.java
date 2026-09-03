package com.adaptivegateway.analytics.entity;

import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import com.adaptivegateway.consumer.entity.ApiConsumer;
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
@Table(name = "analytics_rollups")
public class AnalyticsRollup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bucket_start", nullable = false)
    private Instant bucketStart;

    @Column(name = "bucket_end", nullable = false)
    private Instant bucketEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalyticsGranularity granularity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consumer_id")
    private ApiConsumer consumer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private GatewayRoute route;

    @Column(name = "total_requests", nullable = false)
    private Long totalRequests;

    @Column(name = "allowed_requests", nullable = false)
    private Long allowedRequests;

    @Column(name = "blocked_requests", nullable = false)
    private Long blockedRequests;

    @Column(name = "error_requests", nullable = false)
    private Long errorRequests;

    @Column(name = "avg_response_time_ms", precision = 12, scale = 4)
    private BigDecimal avgResponseTimeMs;

    @Column(name = "p95_response_time_ms", precision = 12, scale = 4)
    private BigDecimal p95ResponseTimeMs;

    @Column(name = "unique_source_ips", nullable = false)
    private Integer uniqueSourceIps;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public Instant getBucketEnd() {
        return bucketEnd;
    }

    public void setBucketEnd(Instant bucketEnd) {
        this.bucketEnd = bucketEnd;
    }

    public AnalyticsGranularity getGranularity() {
        return granularity;
    }

    public void setGranularity(AnalyticsGranularity granularity) {
        this.granularity = granularity;
    }

    public ApiConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(ApiConsumer consumer) {
        this.consumer = consumer;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public void setRoute(GatewayRoute route) {
        this.route = route;
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Long getAllowedRequests() {
        return allowedRequests;
    }

    public void setAllowedRequests(Long allowedRequests) {
        this.allowedRequests = allowedRequests;
    }

    public Long getBlockedRequests() {
        return blockedRequests;
    }

    public void setBlockedRequests(Long blockedRequests) {
        this.blockedRequests = blockedRequests;
    }

    public Long getErrorRequests() {
        return errorRequests;
    }

    public void setErrorRequests(Long errorRequests) {
        this.errorRequests = errorRequests;
    }

    public BigDecimal getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public void setAvgResponseTimeMs(BigDecimal avgResponseTimeMs) {
        this.avgResponseTimeMs = avgResponseTimeMs;
    }

    public BigDecimal getP95ResponseTimeMs() {
        return p95ResponseTimeMs;
    }

    public void setP95ResponseTimeMs(BigDecimal p95ResponseTimeMs) {
        this.p95ResponseTimeMs = p95ResponseTimeMs;
    }

    public Integer getUniqueSourceIps() {
        return uniqueSourceIps;
    }

    public void setUniqueSourceIps(Integer uniqueSourceIps) {
        this.uniqueSourceIps = uniqueSourceIps;
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
