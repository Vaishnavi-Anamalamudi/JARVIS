package com.adaptivegateway.ratelimit.entity;

import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.entity.UpstreamService;
import com.adaptivegateway.ratelimit.enums.GatewayOutcome;
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
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_logs")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "consumer_id")
    private UUID consumerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private GatewayRoute route;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "upstream_service_id")
    private UpstreamService upstreamService;

    @Column(name = "request_method", nullable = false, length = 12)
    private String requestMethod;

    @Column(name = "request_path", nullable = false, length = 2048)
    private String requestPath;

    @Column(name = "request_query_hash", length = 128)
    private String requestQueryHash;

    @Column(name = "request_headers_hash", length = 128)
    private String requestHeadersHash;

    @Column(name = "source_ip")
    private InetAddress sourceIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_outcome", nullable = false, length = 40)
    private GatewayOutcome gatewayOutcome;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "request_bytes", nullable = false)
    private Long requestBytes;

    @Column(name = "response_bytes", nullable = false)
    private Long responseBytes;

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public UpstreamService getUpstreamService() {
        return upstreamService;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestQueryHash() {
        return requestQueryHash;
    }

    public String getRequestHeadersHash() {
        return requestHeadersHash;
    }

    public InetAddress getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public GatewayOutcome getGatewayOutcome() {
        return gatewayOutcome;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public Long getRequestBytes() {
        return requestBytes;
    }

    public Long getResponseBytes() {
        return responseBytes;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
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

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public void setConsumerId(UUID consumerId) {
        this.consumerId = consumerId;
    }

    public void setRoute(GatewayRoute route) {
        this.route = route;
    }

    public void setUpstreamService(UpstreamService upstreamService) {
        this.upstreamService = upstreamService;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public void setRequestQueryHash(String requestQueryHash) {
        this.requestQueryHash = requestQueryHash;
    }

    public void setRequestHeadersHash(String requestHeadersHash) {
        this.requestHeadersHash = requestHeadersHash;
    }

    public void setSourceIp(InetAddress sourceIp) {
        this.sourceIp = sourceIp;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setGatewayOutcome(GatewayOutcome gatewayOutcome) {
        this.gatewayOutcome = gatewayOutcome;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public void setRequestBytes(Long requestBytes) {
        this.requestBytes = requestBytes;
    }

    public void setResponseBytes(Long responseBytes) {
        this.responseBytes = responseBytes;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
