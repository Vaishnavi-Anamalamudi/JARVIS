package com.adaptivegateway.operations.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.operations.dto.AuditLogResponse;
import com.adaptivegateway.operations.dto.RequestLogResponse;
import com.adaptivegateway.operations.mapper.OperationsMapper;
import com.adaptivegateway.operations.repository.AuditLogRepository;
import com.adaptivegateway.ratelimit.enums.GatewayOutcome;
import com.adaptivegateway.ratelimit.repository.RateLimitDecisionRepository;
import com.adaptivegateway.ratelimit.repository.RequestLogRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsQueryService {

    private static final List<String> REQUEST_SORTS = List.of("startedAt", "completedAt", "statusCode", "responseTimeMs", "gatewayOutcome");
    private static final List<String> AUDIT_SORTS = List.of("createdAt", "action", "resourceType");

    private final RequestLogRepository requestLogRepository;
    private final RateLimitDecisionRepository decisionRepository;
    private final AuditLogRepository auditLogRepository;
    private final OperationsMapper mapper;

    public OperationsQueryService(
            RequestLogRepository requestLogRepository,
            RateLimitDecisionRepository decisionRepository,
            AuditLogRepository auditLogRepository,
            OperationsMapper mapper
    ) {
        this.requestLogRepository = requestLogRepository;
        this.decisionRepository = decisionRepository;
        this.auditLogRepository = auditLogRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestLogResponse> listRequests(
            GatewayOutcome outcome,
            UUID routeId,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, REQUEST_SORTS, "startedAt");
        var logs = outcome != null && routeId != null
                ? requestLogRepository.findByGatewayOutcomeAndRouteIdAndDeletedAtIsNull(outcome, routeId, pageRequest)
                : outcome != null
                ? requestLogRepository.findByGatewayOutcomeAndDeletedAtIsNull(outcome, pageRequest)
                : routeId != null
                ? requestLogRepository.findByRouteIdAndDeletedAtIsNull(routeId, pageRequest)
                : requestLogRepository.findByDeletedAtIsNull(pageRequest);
        return PageResponse.from(logs.map(log -> mapper.toResponse(
                log,
                decisionRepository.findByRequestLogIdAndDeletedAtIsNull(log.getId()).orElse(null)
        )));
    }

    @Transactional(readOnly = true)
    public RequestLogResponse getRequest(UUID id) {
        var log = requestLogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Request log does not exist"));
        return mapper.toResponse(log, decisionRepository.findByRequestLogIdAndDeletedAtIsNull(log.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(
            String resourceType,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, AUDIT_SORTS, "createdAt");
        var logs = resourceType == null || resourceType.isBlank()
                ? auditLogRepository.findByDeletedAtIsNull(pageRequest)
                : auditLogRepository.findByResourceTypeAndDeletedAtIsNull(resourceType.trim(), pageRequest);
        return PageResponse.from(logs.map(mapper::toResponse));
    }

    private PageRequest pageRequest(
            int page,
            int size,
            String sort,
            String direction,
            List<String> allowedSorts,
            String defaultSort
    ) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
