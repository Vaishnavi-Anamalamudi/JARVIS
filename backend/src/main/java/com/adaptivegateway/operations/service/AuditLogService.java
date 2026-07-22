package com.adaptivegateway.operations.service;

import com.adaptivegateway.auth.repository.AppUserRepository;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.operations.entity.AuditLog;
import com.adaptivegateway.operations.repository.AuditLogRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ServerWebExchange;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String actorUserId,
            String action,
            String resourceType,
            UUID resourceId,
            ServerWebExchange exchange,
            Map<String, Object> metadata
    ) {
        AuditLog auditLog = new AuditLog();
        if (actorUserId != null) {
            try {
                appUserRepository.findActiveById(UUID.fromString(actorUserId)).ifPresent(auditLog::setActorUser);
            } catch (IllegalArgumentException ignored) {
                auditLog.setActorUser(null);
            }
        }
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setCorrelationId(correlationId(exchange));
        auditLog.setIpAddress(sourceIp(exchange));
        auditLog.setUserAgent(exchange.getRequest().getHeaders().getFirst("User-Agent"));
        auditLog.setMetadata(metadata == null ? Map.of() : new LinkedHashMap<>(metadata));
        auditLogRepository.save(auditLog);
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

    private InetAddress sourceIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null) {
            return null;
        }
        try {
            return InetAddress.getByName(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
