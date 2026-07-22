package com.adaptivegateway.gateway.service;

import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.entity.GatewayRoutePredicate;
import com.adaptivegateway.gateway.enums.RoutePredicateType;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GatewayRouteDefinitionService {

    private final GatewayRouteRepository gatewayRouteRepository;

    public GatewayRouteDefinitionService(GatewayRouteRepository gatewayRouteRepository) {
        this.gatewayRouteRepository = gatewayRouteRepository;
    }

    @Transactional(readOnly = true)
    public List<RouteDefinition> activeRouteDefinitions() {
        return gatewayRouteRepository.findRoutableRoutes()
                .stream()
                .map(this::toRouteDefinition)
                .toList();
    }

    private RouteDefinition toRouteDefinition(GatewayRoute route) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(route.getRouteKey());
        definition.setUri(URI.create(route.getUpstreamService().getBaseUrl()));
        definition.setOrder(route.getPriority());
        definition.setPredicates(route.getPredicates().stream()
                .filter(predicate -> predicate.getDeletedAt() == null)
                .map(this::toPredicateDefinition)
                .toList());
        if (route.getStripPrefix() > 0) {
            definition.setFilters(List.of(new FilterDefinition("StripPrefix=" + route.getStripPrefix())));
        }
        return definition;
    }

    private PredicateDefinition toPredicateDefinition(GatewayRoutePredicate predicate) {
        Map<String, Object> config = predicate.getPredicateConfig();
        RoutePredicateType type = predicate.getPredicateType();
        return switch (type) {
            case PATH -> new PredicateDefinition("Path=" + first(config, "pattern", "patterns"));
            case METHOD -> new PredicateDefinition("Method=" + first(config, "method", "methods"));
            case HOST -> new PredicateDefinition("Host=" + first(config, "pattern", "patterns"));
            case HEADER -> new PredicateDefinition("Header=" + required(config, "name") + optional(config, "regexp"));
            case QUERY -> new PredicateDefinition("Query=" + required(config, "param") + optional(config, "regexp"));
        };
    }

    private String first(Map<String, Object> config, String firstKey, String secondKey) {
        Object firstValue = config.get(firstKey);
        return hasValue(firstValue) ? value(firstValue) : value(config.get(secondKey));
    }

    private String required(Map<String, Object> config, String key) {
        return value(config.get(key));
    }

    private String optional(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return hasValue(value) ? "," + value(value) : "";
    }

    private boolean hasValue(Object value) {
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return value != null;
    }

    private String value(Object value) {
        if (value instanceof Collection<?> collection) {
            return String.join(",", collection.stream().map(Object::toString).toList());
        }
        if (value instanceof String[] array) {
            return String.join(",", array);
        }
        return value.toString();
    }
}
