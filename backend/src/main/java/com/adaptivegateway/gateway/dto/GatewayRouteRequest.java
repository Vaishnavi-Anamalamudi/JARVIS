package com.adaptivegateway.gateway.dto;

import com.adaptivegateway.gateway.enums.GatewayRouteStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record GatewayRouteRequest(
        @NotNull UUID upstreamServiceId,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String routeKey,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 512) @Pattern(regexp = "^/.*") String pathPattern,
        @NotEmpty List<@Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") String> allowedMethods,
        @NotNull @Min(0) Integer stripPrefix,
        @NotNull @Min(0) Integer priority,
        @NotNull GatewayRouteStatus status,
        @Size(max = 10) List<@Valid RoutePredicateRequest> predicates
) {
}
