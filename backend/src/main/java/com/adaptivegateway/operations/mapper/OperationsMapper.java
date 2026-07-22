package com.adaptivegateway.operations.mapper;

import com.adaptivegateway.operations.dto.AuditLogResponse;
import com.adaptivegateway.operations.dto.RateLimitDecisionSnapshotResponse;
import com.adaptivegateway.operations.dto.RequestLogResponse;
import com.adaptivegateway.operations.entity.AuditLog;
import com.adaptivegateway.ratelimit.entity.RateLimitDecision;
import com.adaptivegateway.ratelimit.entity.RequestLog;
import org.springframework.stereotype.Component;

@Component
public class OperationsMapper {

    public RequestLogResponse toResponse(RequestLog log, RateLimitDecision decision) {
        return new RequestLogResponse(
                log.getId(),
                log.getCorrelationId(),
                log.getConsumerId(),
                log.getRoute() == null ? null : log.getRoute().getId(),
                log.getRoute() == null ? null : log.getRoute().getRouteKey(),
                log.getUpstreamService() == null ? null : log.getUpstreamService().getId(),
                log.getUpstreamService() == null ? null : log.getUpstreamService().getName(),
                log.getRequestMethod(),
                log.getRequestPath(),
                log.getRequestQueryHash(),
                log.getRequestHeadersHash(),
                log.getSourceIp() == null ? null : log.getSourceIp().getHostAddress(),
                log.getUserAgent(),
                log.getGatewayOutcome(),
                log.getStatusCode(),
                log.getResponseTimeMs(),
                log.getRequestBytes(),
                log.getResponseBytes(),
                log.getErrorCode(),
                log.getStartedAt(),
                log.getCompletedAt(),
                decision == null ? null : toResponse(decision)
        );
    }

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUser() == null ? null : auditLog.getActorUser().getId(),
                auditLog.getActorUser() == null ? null : auditLog.getActorUser().getUsername(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getCorrelationId(),
                auditLog.getIpAddress() == null ? null : auditLog.getIpAddress().getHostAddress(),
                auditLog.getUserAgent(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt()
        );
    }

    private RateLimitDecisionSnapshotResponse toResponse(RateLimitDecision decision) {
        return new RateLimitDecisionSnapshotResponse(
                decision.getId(),
                decision.getPolicy().getId(),
                decision.getAssignment() == null ? null : decision.getAssignment().getId(),
                decision.getAlgorithm(),
                decision.getDecision(),
                decision.getEffectiveLimit(),
                decision.getObservedCount(),
                decision.getRemainingTokens(),
                decision.getRetryAfterSeconds(),
                decision.getStrictnessFactor(),
                decision.getCreatedAt()
        );
    }
}
