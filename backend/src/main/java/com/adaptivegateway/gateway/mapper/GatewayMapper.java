package com.adaptivegateway.gateway.mapper;

import com.adaptivegateway.gateway.dto.GatewayRouteResponse;
import com.adaptivegateway.gateway.dto.RoutePredicateResponse;
import com.adaptivegateway.gateway.dto.UpstreamServiceResponse;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.entity.GatewayRoutePredicate;
import com.adaptivegateway.gateway.entity.UpstreamService;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GatewayMapper {

    public UpstreamServiceResponse toResponse(UpstreamService upstreamService) {
        return new UpstreamServiceResponse(
                upstreamService.getId(),
                upstreamService.getName(),
                upstreamService.getBaseUrl(),
                upstreamService.getHealthCheckPath(),
                upstreamService.getStatus(),
                upstreamService.getTimeoutMs(),
                upstreamService.getRetryCount(),
                upstreamService.getCreatedAt(),
                upstreamService.getUpdatedAt()
        );
    }

    public GatewayRouteResponse toResponse(GatewayRoute route) {
        return new GatewayRouteResponse(
                route.getId(),
                route.getUpstreamService().getId(),
                route.getUpstreamService().getName(),
                route.getRouteKey(),
                route.getName(),
                route.getPathPattern(),
                Arrays.asList(route.getAllowedMethods()),
                route.getStripPrefix(),
                route.getPriority(),
                route.getStatus(),
                route.getPredicates().stream()
                        .filter(predicate -> predicate.getDeletedAt() == null)
                        .map(this::toResponse)
                        .toList(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }

    private RoutePredicateResponse toResponse(GatewayRoutePredicate predicate) {
        return new RoutePredicateResponse(
                predicate.getId(),
                predicate.getPredicateType(),
                predicate.getPredicateConfig()
        );
    }
}
