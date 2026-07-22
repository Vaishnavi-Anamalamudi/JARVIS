package com.adaptivegateway.gateway.controller;

import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.gateway.dto.GatewayRouteRequest;
import com.adaptivegateway.gateway.dto.GatewayRouteResponse;
import com.adaptivegateway.gateway.dto.UpstreamServiceRequest;
import com.adaptivegateway.gateway.dto.UpstreamServiceResponse;
import com.adaptivegateway.gateway.enums.GatewayRouteStatus;
import com.adaptivegateway.gateway.enums.UpstreamServiceStatus;
import com.adaptivegateway.gateway.service.GatewayManagementService;
import com.adaptivegateway.gateway.service.GatewayRouteRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/gateway")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Gateway Management")
public class GatewayManagementController {

    private final GatewayManagementService gatewayManagementService;
    private final GatewayRouteRefreshService routeRefreshService;

    public GatewayManagementController(
            GatewayManagementService gatewayManagementService,
            GatewayRouteRefreshService routeRefreshService
    ) {
        this.gatewayManagementService = gatewayManagementService;
        this.routeRefreshService = routeRefreshService;
    }

    @GetMapping("/upstreams")
    @Operation(summary = "List registered upstream services")
    public ApiResponse<PageResponse<UpstreamServiceResponse>> listUpstreams(
            @RequestParam(required = false) UpstreamServiceStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        var response = gatewayManagementService.listUpstreams(status, page, size, sort, direction);
        return ApiResponse.success(correlationId, "Upstream services read successfully", response);
    }

    @GetMapping("/upstreams/{id}")
    @Operation(summary = "Read one upstream service")
    public ApiResponse<UpstreamServiceResponse> getUpstream(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Upstream service read successfully", gatewayManagementService.getUpstream(id));
    }

    @PostMapping("/upstreams")
    @Operation(summary = "Register an upstream service")
    public ApiResponse<UpstreamServiceResponse> createUpstream(
            @Valid @RequestBody UpstreamServiceRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Upstream service created", gatewayManagementService.createUpstream(request));
    }

    @PutMapping("/upstreams/{id}")
    @Operation(summary = "Update an upstream service")
    public ApiResponse<UpstreamServiceResponse> updateUpstream(
            @PathVariable UUID id,
            @Valid @RequestBody UpstreamServiceRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Upstream service updated", gatewayManagementService.updateUpstream(id, request));
    }

    @DeleteMapping("/upstreams/{id}")
    @Operation(summary = "Soft-delete an upstream service")
    public ApiResponse<Void> deleteUpstream(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        gatewayManagementService.deleteUpstream(id);
        return ApiResponse.success(correlationId, "Upstream service deleted", null);
    }

    @GetMapping("/routes")
    @Operation(summary = "List gateway routes")
    public ApiResponse<PageResponse<GatewayRouteResponse>> listRoutes(
            @RequestParam(required = false) GatewayRouteStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "priority") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        var response = gatewayManagementService.listRoutes(status, page, size, sort, direction);
        return ApiResponse.success(correlationId, "Gateway routes read successfully", response);
    }

    @GetMapping("/routes/{id}")
    @Operation(summary = "Read one gateway route")
    public ApiResponse<GatewayRouteResponse> getRoute(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Gateway route read successfully", gatewayManagementService.getRoute(id));
    }

    @PostMapping("/routes")
    @Operation(summary = "Create a gateway route")
    public ApiResponse<GatewayRouteResponse> createRoute(
            @Valid @RequestBody GatewayRouteRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Gateway route created", gatewayManagementService.createRoute(request));
    }

    @PutMapping("/routes/{id}")
    @Operation(summary = "Update a gateway route")
    public ApiResponse<GatewayRouteResponse> updateRoute(
            @PathVariable UUID id,
            @Valid @RequestBody GatewayRouteRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Gateway route updated", gatewayManagementService.updateRoute(id, request));
    }

    @DeleteMapping("/routes/{id}")
    @Operation(summary = "Soft-delete a gateway route")
    public ApiResponse<Void> deleteRoute(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        gatewayManagementService.deleteRoute(id);
        return ApiResponse.success(correlationId, "Gateway route deleted", null);
    }

    @PostMapping("/routes/refresh")
    @Operation(summary = "Refresh Spring Cloud Gateway route definitions from PostgreSQL")
    public ApiResponse<Void> refreshRoutes(
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        routeRefreshService.refreshRoutes();
        return ApiResponse.success(correlationId, "Gateway routes refreshed", null);
    }
}
