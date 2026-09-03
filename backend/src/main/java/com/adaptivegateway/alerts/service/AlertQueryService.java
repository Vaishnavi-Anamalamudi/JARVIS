package com.adaptivegateway.alerts.service;

import com.adaptivegateway.alerts.dto.AlertResponse;
import com.adaptivegateway.alerts.entity.Alert;
import com.adaptivegateway.alerts.enums.AlertStatus;
import com.adaptivegateway.alerts.mapper.AlertMapper;
import com.adaptivegateway.alerts.repository.AlertRepository;
import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.repository.AppUserRepository;
import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.operations.service.AuditLogService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ServerWebExchange;

@Service
public class AlertQueryService {

    private static final List<String> ALERT_SORTS = List.of("createdAt", "updatedAt", "severity", "status", "alertType");

    private final AlertRepository alertRepository;
    private final AppUserRepository appUserRepository;
    private final AlertMapper mapper;
    private final AuditLogService auditLogService;
    private final LiveEventService liveEventService;

    public AlertQueryService(
            AlertRepository alertRepository,
            AppUserRepository appUserRepository,
            AlertMapper mapper,
            AuditLogService auditLogService,
            LiveEventService liveEventService
    ) {
        this.alertRepository = alertRepository;
        this.appUserRepository = appUserRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.liveEventService = liveEventService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> listAlerts(
            AlertStatus status,
            UUID routeId,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, ALERT_SORTS, "createdAt");
        var alerts = status != null && routeId != null
                ? alertRepository.findByStatusAndRouteIdAndDeletedAtIsNull(status, routeId, pageRequest)
                : status != null
                ? alertRepository.findByStatusAndDeletedAtIsNull(status, pageRequest)
                : routeId != null
                ? alertRepository.findByRouteIdAndDeletedAtIsNull(routeId, pageRequest)
                : alertRepository.findByDeletedAtIsNull(pageRequest);
        return PageResponse.from(alerts.map(mapper::toResponse));
    }

    @Transactional
    public AlertResponse acknowledge(UUID alertId, Jwt actor, ServerWebExchange exchange) {
        Alert alert = findAlert(alertId);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Resolved alerts cannot be acknowledged");
        }
        if (alert.getStatus() == AlertStatus.ACKNOWLEDGED) {
            return mapper.toResponse(alert);
        }

        AppUser user = findActor(actor);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedByUser(user);
        alert.setAcknowledgedAt(Instant.now());
        Alert saved = alertRepository.save(alert);

        auditLogService.record(actor.getSubject(), "ALERT_ACKNOWLEDGED", "alert", saved.getId(), exchange, metadata(saved));
        emit("gateway.alert.acknowledged", "Alert acknowledged", saved, exchange);
        return mapper.toResponse(saved);
    }

    @Transactional
    public AlertResponse resolve(UUID alertId, Jwt actor, ServerWebExchange exchange) {
        Alert alert = findAlert(alertId);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return mapper.toResponse(alert);
        }

        AppUser user = findActor(actor);
        if (alert.getAcknowledgedByUser() == null) {
            alert.setAcknowledgedByUser(user);
            alert.setAcknowledgedAt(Instant.now());
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        Alert saved = alertRepository.save(alert);

        auditLogService.record(actor.getSubject(), "ALERT_RESOLVED", "alert", saved.getId(), exchange, metadata(saved));
        emit("gateway.alert.resolved", "Alert resolved", saved, exchange);
        return mapper.toResponse(saved);
    }

    private Alert findAlert(UUID alertId) {
        return alertRepository.findByIdAndDeletedAtIsNull(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Alert does not exist"));
    }

    private AppUser findActor(Jwt actor) {
        try {
            return appUserRepository.findActiveById(UUID.fromString(actor.getSubject()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authenticated user does not exist"));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token subject is invalid");
        }
    }

    private Map<String, Object> metadata(Alert alert) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", alert.getStatus().name());
        metadata.put("severity", alert.getSeverity().name());
        metadata.put("alertType", alert.getAlertType());
        metadata.put("routeId", alert.getRoute() == null ? null : alert.getRoute().getId().toString());
        return metadata;
    }

    private void emit(String type, String message, Alert alert, ServerWebExchange exchange) {
        liveEventService.emit(
                type,
                "alert",
                alert.getId(),
                correlationId(exchange),
                message,
                metadata(alert)
        );
    }

    private UUID correlationId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private PageRequest pageRequest(int page, int size, String sort, String direction, List<String> allowedSorts, String defaultSort) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
