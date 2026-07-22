package com.adaptivegateway.gateway.config;

import com.adaptivegateway.gateway.service.GatewayRouteDefinitionService;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class DatabaseRouteDefinitionLocator implements RouteDefinitionLocator {

    private final GatewayRouteDefinitionService routeDefinitionService;

    public DatabaseRouteDefinitionLocator(GatewayRouteDefinitionService routeDefinitionService) {
        this.routeDefinitionService = routeDefinitionService;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Mono.fromCallable(routeDefinitionService::activeRouteDefinitions)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }
}
