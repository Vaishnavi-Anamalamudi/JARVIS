package com.adaptivegateway.gateway.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.gateway.dto.GatewayRouteRequest;
import com.adaptivegateway.gateway.dto.GatewayRouteResponse;
import com.adaptivegateway.gateway.dto.RoutePredicateRequest;
import com.adaptivegateway.gateway.dto.UpstreamServiceRequest;
import com.adaptivegateway.gateway.dto.UpstreamServiceResponse;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.entity.GatewayRoutePredicate;
import com.adaptivegateway.gateway.entity.UpstreamService;
import com.adaptivegateway.gateway.enums.GatewayRouteStatus;
import com.adaptivegateway.gateway.enums.RoutePredicateType;
import com.adaptivegateway.gateway.enums.UpstreamServiceStatus;
import com.adaptivegateway.gateway.mapper.GatewayMapper;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.gateway.repository.UpstreamServiceRepository;
import com.adaptivegateway.live.service.LiveEventService;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GatewayManagementService {

    private static final List<String> UPSTREAM_SORTS = List.of("createdAt", "updatedAt", "name", "status");
    private static final List<String> ROUTE_SORTS = List.of("createdAt", "updatedAt", "routeKey", "priority", "status");

    private final UpstreamServiceRepository upstreamServiceRepository;
    private final GatewayRouteRepository gatewayRouteRepository;
    private final GatewayMapper gatewayMapper;
    private final GatewayRouteRefreshService routeRefreshService;
    private final LiveEventService liveEventService;

    public GatewayManagementService(
            UpstreamServiceRepository upstreamServiceRepository,
            GatewayRouteRepository gatewayRouteRepository,
            GatewayMapper gatewayMapper,
            GatewayRouteRefreshService routeRefreshService,
            LiveEventService liveEventService
    ) {
        this.upstreamServiceRepository = upstreamServiceRepository;
        this.gatewayRouteRepository = gatewayRouteRepository;
        this.gatewayMapper = gatewayMapper;
        this.routeRefreshService = routeRefreshService;
        this.liveEventService = liveEventService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UpstreamServiceResponse> listUpstreams(
            UpstreamServiceStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, UPSTREAM_SORTS, "createdAt");
        var upstreams = status == null
                ? upstreamServiceRepository.findByDeletedAtIsNull(pageRequest)
                : upstreamServiceRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        return PageResponse.from(upstreams.map(gatewayMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UpstreamServiceResponse getUpstream(UUID id) {
        return gatewayMapper.toResponse(findUpstream(id));
    }

    @Transactional
    public UpstreamServiceResponse createUpstream(UpstreamServiceRequest request) {
        String name = request.name().trim();
        if (upstreamServiceRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Upstream service name is already registered");
        }

        UpstreamService upstreamService = new UpstreamService();
        apply(request, upstreamService);
        UpstreamService saved = upstreamServiceRepository.save(upstreamService);
        routeRefreshService.refreshRoutes();
        emitGatewayChange("UPSTREAM_CREATED", "upstream_service", saved.getId(), saved.getName());
        return gatewayMapper.toResponse(saved);
    }

    @Transactional
    public UpstreamServiceResponse updateUpstream(UUID id, UpstreamServiceRequest request) {
        UpstreamService upstreamService = findUpstream(id);
        String requestedName = request.name().trim();
        if (!upstreamService.getName().equalsIgnoreCase(requestedName)
                && upstreamServiceRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(requestedName)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Upstream service name is already registered");
        }

        apply(request, upstreamService);
        UpstreamService saved = upstreamServiceRepository.save(upstreamService);
        routeRefreshService.refreshRoutes();
        emitGatewayChange("UPSTREAM_UPDATED", "upstream_service", saved.getId(), saved.getName());
        return gatewayMapper.toResponse(saved);
    }

    @Transactional
    public void deleteUpstream(UUID id) {
        UpstreamService upstreamService = findUpstream(id);
        upstreamService.setDeletedAt(Instant.now());
        upstreamServiceRepository.save(upstreamService);
        routeRefreshService.refreshRoutes();
        emitGatewayChange("UPSTREAM_DELETED", "upstream_service", upstreamService.getId(), upstreamService.getName());
    }

    @Transactional(readOnly = true)
    public PageResponse<GatewayRouteResponse> listRoutes(
            GatewayRouteStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, ROUTE_SORTS, "priority");
        var routes = status == null
                ? gatewayRouteRepository.findByDeletedAtIsNull(pageRequest)
                : gatewayRouteRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        return PageResponse.from(routes.map(gatewayMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public GatewayRouteResponse getRoute(UUID id) {
        return gatewayMapper.toResponse(findRoute(id));
    }

    @Transactional
    public GatewayRouteResponse createRoute(GatewayRouteRequest request) {
        String routeKey = normalizeRouteKey(request.routeKey());
        if (gatewayRouteRepository.existsByRouteKeyIgnoreCaseAndDeletedAtIsNull(routeKey)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Gateway route key is already registered");
        }

        GatewayRoute route = new GatewayRoute();
        route.setRouteKey(routeKey);
        apply(request, route);
        GatewayRoute saved = gatewayRouteRepository.save(route);
        routeRefreshService.refreshRoutes();
        emitGatewayChange("ROUTE_CREATED", "gateway_route", saved.getId(), saved.getRouteKey());
        return gatewayMapper.toResponse(saved);
    }

    @Transactional
    public GatewayRouteResponse updateRoute(UUID id, GatewayRouteRequest request) {
        GatewayRoute route = findRoute(id);
        String routeKey = normalizeRouteKey(request.routeKey());
        if (!route.getRouteKey().equalsIgnoreCase(routeKey)
                && gatewayRouteRepository.existsByRouteKeyIgnoreCaseAndDeletedAtIsNull(routeKey)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Gateway route key is already registered");
        }

        route.setRouteKey(routeKey);
        apply(request, route);
        GatewayRoute saved = gatewayRouteRepository.save(route);
        routeRefreshService.refreshRoutes();
        emitGatewayChange("ROUTE_UPDATED", "gateway_route", saved.getId(), saved.getRouteKey());
        return gatewayMapper.toResponse(saved);
    }

    @Transactional
    public void deleteRoute(UUID id) {
        GatewayRoute route = findRoute(id);
        route.setDeletedAt(Instant.now());
        gatewayRouteRepository.save(route);
        routeRefreshService.refreshRoutes();
        emitGatewayChange("ROUTE_DELETED", "gateway_route", route.getId(), route.getRouteKey());
    }

    private void apply(UpstreamServiceRequest request, UpstreamService upstreamService) {
        URI.create(request.baseUrl().trim());
        upstreamService.setName(request.name().trim());
        upstreamService.setBaseUrl(request.baseUrl().trim());
        upstreamService.setHealthCheckPath(blankToNull(request.healthCheckPath()));
        upstreamService.setStatus(request.status());
        upstreamService.setTimeoutMs(request.timeoutMs());
        upstreamService.setRetryCount(request.retryCount());
    }

    private void apply(GatewayRouteRequest request, GatewayRoute route) {
        UpstreamService upstreamService = findUpstream(request.upstreamServiceId());
        route.setUpstreamService(upstreamService);
        route.setName(request.name().trim());
        route.setPathPattern(request.pathPattern().trim());
        route.setAllowedMethods(normalizedMethods(request.allowedMethods()));
        route.setStripPrefix(request.stripPrefix());
        route.setPriority(request.priority());
        route.setStatus(request.status());
        route.replacePredicates(toPredicates(request));
    }

    private List<GatewayRoutePredicate> toPredicates(GatewayRouteRequest request) {
        List<GatewayRoutePredicate> predicates = new ArrayList<>();
        predicates.add(predicate(RoutePredicateType.PATH, Map.of("pattern", request.pathPattern().trim())));
        predicates.add(predicate(RoutePredicateType.METHOD, Map.of("methods", List.of(normalizedMethods(request.allowedMethods())))));

        if (request.predicates() != null) {
            request.predicates().forEach(extraPredicate -> {
                validatePredicateConfig(extraPredicate);
                if (extraPredicate.type() != RoutePredicateType.PATH && extraPredicate.type() != RoutePredicateType.METHOD) {
                    predicates.add(predicate(extraPredicate.type(), extraPredicate.config()));
                }
            });
        }
        return predicates;
    }

    private GatewayRoutePredicate predicate(RoutePredicateType type, Map<String, Object> config) {
        GatewayRoutePredicate predicate = new GatewayRoutePredicate();
        predicate.setPredicateType(type);
        predicate.setPredicateConfig(new LinkedHashMap<>(config));
        return predicate;
    }

    private void validatePredicateConfig(RoutePredicateRequest request) {
        switch (request.type()) {
            case PATH -> requireAny(request.config(), "pattern", "patterns");
            case METHOD -> requireAny(request.config(), "method", "methods");
            case HOST -> requireAny(request.config(), "pattern", "patterns");
            case HEADER -> requireAll(request.config(), "name");
            case QUERY -> requireAll(request.config(), "param");
        }
    }

    private void requireAny(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            if (hasValue(config.get(key))) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Gateway predicate configuration is incomplete");
    }

    private void requireAll(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            if (!hasValue(config.get(key))) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Gateway predicate configuration is incomplete");
            }
        }
    }

    private boolean hasValue(Object value) {
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable.iterator().hasNext();
        }
        return value != null;
    }

    private UpstreamService findUpstream(UUID id) {
        return upstreamServiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Upstream service does not exist"));
    }

    private GatewayRoute findRoute(UUID id) {
        return gatewayRouteRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Gateway route does not exist"));
    }

    private String[] normalizedMethods(List<String> methods) {
        return methods.stream()
                .map(method -> method.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toArray(String[]::new);
    }

    private String normalizeRouteKey(String routeKey) {
        return routeKey.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void emitGatewayChange(String action, String resourceType, UUID resourceId, String name) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("resourceType", resourceType);
        payload.put("resourceId", resourceId == null ? null : resourceId.toString());
        payload.put("name", name);
        liveEventService.emit(
                "gateway.config.changed",
                resourceType,
                resourceId,
                null,
                "Gateway configuration changed",
                payload
        );
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
