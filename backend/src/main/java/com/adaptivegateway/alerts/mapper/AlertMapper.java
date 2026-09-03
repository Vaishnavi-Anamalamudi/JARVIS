package com.adaptivegateway.alerts.mapper;

import com.adaptivegateway.alerts.dto.AlertResponse;
import com.adaptivegateway.alerts.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getAnomalyRecord() == null ? null : alert.getAnomalyRecord().getId(),
                alert.getRoute() == null ? null : alert.getRoute().getId(),
                alert.getRoute() == null ? null : alert.getRoute().getRouteKey(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getAcknowledgedByUser() == null ? null : alert.getAcknowledgedByUser().getId(),
                alert.getAcknowledgedByUser() == null ? null : alert.getAcknowledgedByUser().getUsername(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }
}
