package com.adaptivegateway.alerts.controller;

import com.adaptivegateway.alerts.dto.AlertResponse;
import com.adaptivegateway.alerts.enums.AlertStatus;
import com.adaptivegateway.alerts.service.AlertQueryService;
import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/alerts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Alerts")
public class AlertController {

    private final AlertQueryService alertQueryService;

    public AlertController(AlertQueryService alertQueryService) {
        this.alertQueryService = alertQueryService;
    }

    @GetMapping
    @Operation(summary = "List alert records")
    public ApiResponse<PageResponse<AlertResponse>> alerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        var response = alertQueryService.listAlerts(status, routeId, page, size, sort, direction);
        return ApiResponse.success(correlationId, "Alerts read successfully", response);
    }
}
