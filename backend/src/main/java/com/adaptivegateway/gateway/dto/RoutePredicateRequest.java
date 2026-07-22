package com.adaptivegateway.gateway.dto;

import com.adaptivegateway.gateway.enums.RoutePredicateType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RoutePredicateRequest(
        @NotNull RoutePredicateType type,
        @NotEmpty Map<String, Object> config
) {
}
