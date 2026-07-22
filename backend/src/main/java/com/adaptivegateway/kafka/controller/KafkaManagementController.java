package com.adaptivegateway.kafka.controller;

import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.kafka.dto.KafkaOutboxEventResponse;
import com.adaptivegateway.kafka.dto.KafkaPublishSummaryResponse;
import com.adaptivegateway.kafka.enums.KafkaOutboxStatus;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import com.adaptivegateway.kafka.service.KafkaOutboxQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/kafka")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Kafka Management")
public class KafkaManagementController {

    private final KafkaOutboxQueryService queryService;
    private final KafkaEventPublisherService publisherService;

    public KafkaManagementController(KafkaOutboxQueryService queryService, KafkaEventPublisherService publisherService) {
        this.queryService = queryService;
        this.publisherService = publisherService;
    }

    @GetMapping("/outbox")
    @Operation(summary = "List Kafka outbox events")
    public ApiResponse<PageResponse<KafkaOutboxEventResponse>> listEvents(
            @RequestParam(required = false) KafkaOutboxStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        var response = queryService.listEvents(status, page, size, sort, direction);
        return ApiResponse.success(correlationId, "Kafka outbox events read successfully", response);
    }

    @GetMapping("/outbox/{id}")
    @Operation(summary = "Read one Kafka outbox event")
    public ApiResponse<KafkaOutboxEventResponse> getEvent(
            @PathVariable UUID id,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Kafka outbox event read successfully", queryService.getEvent(id));
    }

    @PostMapping("/outbox/publish-pending")
    @Operation(summary = "Publish due pending Kafka outbox events")
    public ApiResponse<KafkaPublishSummaryResponse> publishPending(
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        return ApiResponse.success(correlationId, "Kafka outbox publish attempt completed", publisherService.publishDueEvents());
    }
}
