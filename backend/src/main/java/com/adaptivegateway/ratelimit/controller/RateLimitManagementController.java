package com.adaptivegateway.ratelimit.controller;

import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.ratelimit.dto.RateLimitAssignmentRequest;
import com.adaptivegateway.ratelimit.dto.RateLimitAssignmentResponse;
import com.adaptivegateway.ratelimit.dto.RateLimitPolicyRequest;
import com.adaptivegateway.ratelimit.dto.RateLimitPolicyResponse;
import com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import com.adaptivegateway.ratelimit.service.RateLimitManagementService;
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
@RequestMapping("/api/rate-limit")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rate Limit Management")
public class RateLimitManagementController {

    private final RateLimitManagementService service;

    public RateLimitManagementController(RateLimitManagementService service) {
        this.service = service;
    }

    @GetMapping("/policies")
    @Operation(summary = "List rate limit policies")
    public ApiResponse<PageResponse<RateLimitPolicyResponse>> listPolicies(
            @RequestParam(required = false) RateLimitPolicyStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit policies read successfully",
                service.listPolicies(status, page, size, sort, direction));
    }

    @GetMapping("/policies/{id}")
    @Operation(summary = "Read one rate limit policy")
    public ApiResponse<RateLimitPolicyResponse> getPolicy(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit policy read successfully", service.getPolicy(id));
    }

    @PostMapping("/policies")
    @Operation(summary = "Create a rate limit policy")
    public ApiResponse<RateLimitPolicyResponse> createPolicy(
            @Valid @RequestBody RateLimitPolicyRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit policy created", service.createPolicy(request));
    }

    @PutMapping("/policies/{id}")
    @Operation(summary = "Update a rate limit policy")
    public ApiResponse<RateLimitPolicyResponse> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody RateLimitPolicyRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit policy updated", service.updatePolicy(id, request));
    }

    @DeleteMapping("/policies/{id}")
    @Operation(summary = "Soft-delete a rate limit policy")
    public ApiResponse<Void> deletePolicy(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        service.deletePolicy(id);
        return ApiResponse.success(correlationId, "Rate limit policy deleted", null);
    }

    @GetMapping("/assignments")
    @Operation(summary = "List rate limit assignments")
    public ApiResponse<PageResponse<RateLimitAssignmentResponse>> listAssignments(
            @RequestParam(required = false) RateLimitAssignmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "priority") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit assignments read successfully",
                service.listAssignments(status, page, size, sort, direction));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Read one rate limit assignment")
    public ApiResponse<RateLimitAssignmentResponse> getAssignment(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit assignment read successfully", service.getAssignment(id));
    }

    @PostMapping("/assignments")
    @Operation(summary = "Create a route-scoped rate limit assignment")
    public ApiResponse<RateLimitAssignmentResponse> createAssignment(
            @Valid @RequestBody RateLimitAssignmentRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit assignment created", service.createAssignment(request));
    }

    @PutMapping("/assignments/{id}")
    @Operation(summary = "Update a route-scoped rate limit assignment")
    public ApiResponse<RateLimitAssignmentResponse> updateAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody RateLimitAssignmentRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Rate limit assignment updated", service.updateAssignment(id, request));
    }

    @DeleteMapping("/assignments/{id}")
    @Operation(summary = "Soft-delete a rate limit assignment")
    public ApiResponse<Void> deleteAssignment(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        service.deleteAssignment(id);
        return ApiResponse.success(correlationId, "Rate limit assignment deleted", null);
    }
}
