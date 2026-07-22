package com.adaptivegateway.operations.dto;

import com.adaptivegateway.ratelimit.enums.GatewayOutcome;
import java.time.Instant;
import java.util.UUID;

public record RequestLogResponse(
        UUID id,
        UUID correlationId,
        UUID consumerId,
        UUID routeId,
        String routeKey,
        UUID upstreamServiceId,
        String upstreamServiceName,
        String requestMethod,
        String requestPath,
        String requestQueryHash,
        String requestHeadersHash,
        String sourceIp,
        String userAgent,
        GatewayOutcome gatewayOutcome,
        Integer statusCode,
        Integer responseTimeMs,
        Long requestBytes,
        Long responseBytes,
        String errorCode,
        Instant startedAt,
        Instant completedAt,
        RateLimitDecisionSnapshotResponse rateLimitDecision
) {
}
