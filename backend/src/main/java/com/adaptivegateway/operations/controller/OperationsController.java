package com.adaptivegateway.operations.controller;

import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.operations.dto.AuditLogResponse;
import com.adaptivegateway.operations.dto.RequestLogResponse;
import com.adaptivegateway.operations.service.OperationsQueryService;
import com.adaptivegateway.ratelimit.enums.GatewayOutcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/operations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Operations")
public class OperationsController {

    private final OperationsQueryService service;

    public OperationsController(OperationsQueryService service) {
        this.service = service;
    }

    @GetMapping("/requests")
    @Operation(summary = "List gateway request history")
    public ApiResponse<PageResponse<RequestLogResponse>> listRequests(
            @RequestParam(required = false) GatewayOutcome outcome,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "startedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Gateway request history read successfully",
                service.listRequests(outcome, routeId, page, size, sort, direction));
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Read one gateway request record")
    public ApiResponse<RequestLogResponse> getRequest(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Gateway request record read successfully", service.getRequest(id));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List audit logs")
    public ApiResponse<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Audit logs read successfully",
                service.listAuditLogs(resourceType, page, size, sort, direction));
    }
}
