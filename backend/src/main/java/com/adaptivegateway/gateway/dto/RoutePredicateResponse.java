package com.adaptivegateway.gateway.dto;

import com.adaptivegateway.gateway.enums.RoutePredicateType;
import java.util.Map;
import java.util.UUID;

public record RoutePredicateResponse(
        UUID id,
        RoutePredicateType type,
        Map<String, Object> config
) {
}
