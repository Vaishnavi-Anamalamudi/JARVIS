package com.adaptivegateway.gateway.dto;

import com.adaptivegateway.gateway.enums.GatewayRouteStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GatewayRouteResponse(
        UUID id,
        UUID upstreamServiceId,
        String upstreamServiceName,
        String routeKey,
        String name,
        String pathPattern,
        List<String> allowedMethods,
        Integer stripPrefix,
        Integer priority,
        GatewayRouteStatus status,
        List<RoutePredicateResponse> predicates,
        Instant createdAt,
        Instant updatedAt
) {
}
