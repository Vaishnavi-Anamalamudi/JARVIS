package com.adaptivegateway.alerts.service;

import com.adaptivegateway.alerts.dto.AlertResponse;
import com.adaptivegateway.alerts.enums.AlertStatus;
import com.adaptivegateway.alerts.mapper.AlertMapper;
import com.adaptivegateway.alerts.repository.AlertRepository;
import com.adaptivegateway.common.pagination.PageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertQueryService {

    private static final List<String> ALERT_SORTS = List.of("createdAt", "updatedAt", "severity", "status", "alertType");

    private final AlertRepository alertRepository;
    private final AlertMapper mapper;

    public AlertQueryService(AlertRepository alertRepository, AlertMapper mapper) {
        this.alertRepository = alertRepository;
        this.mapper = mapper;
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

    private PageRequest pageRequest(int page, int size, String sort, String direction, List<String> allowedSorts, String defaultSort) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
