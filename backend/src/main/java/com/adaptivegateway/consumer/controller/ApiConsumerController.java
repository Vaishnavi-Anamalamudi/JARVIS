package com.adaptivegateway.consumer.controller;

import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.consumer.dto.ApiConsumerRequest;
import com.adaptivegateway.consumer.dto.ApiConsumerResponse;
import com.adaptivegateway.consumer.dto.ApiCredentialResponse;
import com.adaptivegateway.consumer.dto.CreateCredentialRequest;
import com.adaptivegateway.consumer.dto.CreateCredentialResponse;
import com.adaptivegateway.consumer.enums.ApiConsumerStatus;
import com.adaptivegateway.consumer.service.ApiConsumerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import org.springframework.web.server.ServerWebExchange;

@Validated
@RestController
@RequestMapping("/api/consumers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "API Consumers")
public class ApiConsumerController {

    private final ApiConsumerService service;

    public ApiConsumerController(ApiConsumerService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List API consumers")
    public ApiResponse<PageResponse<ApiConsumerResponse>> listConsumers(
            @RequestParam(required = false) ApiConsumerStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "API consumers read successfully",
                service.listConsumers(status, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one API consumer")
    public ApiResponse<ApiConsumerResponse> getConsumer(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "API consumer read successfully", service.getConsumer(id));
    }

    @PostMapping
    @Operation(summary = "Create an API consumer")
    public ApiResponse<ApiConsumerResponse> createConsumer(
            @Valid @RequestBody ApiConsumerRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        return ApiResponse.success(correlationId, "API consumer created", service.createConsumer(request, jwt, exchange));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an API consumer")
    public ApiResponse<ApiConsumerResponse> updateConsumer(
            @PathVariable UUID id,
            @Valid @RequestBody ApiConsumerRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        return ApiResponse.success(correlationId, "API consumer updated", service.updateConsumer(id, request, jwt, exchange));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an API consumer")
    public ApiResponse<Void> deleteConsumer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        service.deleteConsumer(id, jwt, exchange);
        return ApiResponse.success(correlationId, "API consumer deleted", null);
    }

    @GetMapping("/{consumerId}/credentials")
    @Operation(summary = "List API consumer credentials")
    public ApiResponse<PageResponse<ApiCredentialResponse>> listCredentials(
            @PathVariable UUID consumerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "API consumer credentials read successfully",
                service.listCredentials(consumerId, page, size));
    }

    @PostMapping("/{consumerId}/credentials")
    @Operation(summary = "Create an API consumer credential")
    public ApiResponse<CreateCredentialResponse> createCredential(
            @PathVariable UUID consumerId,
            @Valid @RequestBody CreateCredentialRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        return ApiResponse.success(correlationId, "API consumer credential created",
                service.createCredential(consumerId, request, jwt, exchange));
    }

    @PostMapping("/credentials/{credentialId}/revoke")
    @Operation(summary = "Revoke an API consumer credential")
    public ApiResponse<ApiCredentialResponse> revokeCredential(
            @PathVariable UUID credentialId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        return ApiResponse.success(correlationId, "API consumer credential revoked",
                service.revokeCredential(credentialId, jwt, exchange));
    }
}
