package com.adaptivegateway.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.gateway.dto.GatewayRouteRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private UpstreamServiceRepository upstreamServiceRepository;

    @Mock
    private GatewayRouteRepository gatewayRouteRepository;

    @Mock
    private GatewayRouteRefreshService routeRefreshService;

    @Mock
    private LiveEventService liveEventService;

    @Test
    void createRoutePersistsCanonicalPathAndMethodPredicates() {
        UpstreamService upstream = upstream("orders-api", "https://orders.example.com");
        when(upstreamServiceRepository.findByIdAndDeletedAtIsNull(upstream.getId())).thenReturn(Optional.of(upstream));
        when(gatewayRouteRepository.existsByRouteKeyIgnoreCaseAndDeletedAtIsNull("orders")).thenReturn(false);
        when(gatewayRouteRepository.save(any(GatewayRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GatewayManagementService service = new GatewayManagementService(
                upstreamServiceRepository,
                gatewayRouteRepository,
                new GatewayMapper(),
                routeRefreshService,
                liveEventService
        );

        service.createRoute(new GatewayRouteRequest(
                upstream.getId(),
                "Orders",
                "Orders Route",
                "/proxy/orders/**",
                List.of("GET", "POST", "GET"),
                1,
                5,
                GatewayRouteStatus.ACTIVE,
                List.of()
        ));

        ArgumentCaptor<GatewayRoute> routeCaptor = ArgumentCaptor.forClass(GatewayRoute.class);
        verify(gatewayRouteRepository).save(routeCaptor.capture());
        GatewayRoute savedRoute = routeCaptor.getValue();
        assertThat(savedRoute.getRouteKey()).isEqualTo("orders");
        assertThat(savedRoute.getAllowedMethods()).containsExactly("GET", "POST");
        assertThat(savedRoute.getPredicates())
                .extracting(GatewayRoutePredicate::getPredicateType)
                .containsExactly(RoutePredicateType.PATH, RoutePredicateType.METHOD);
        verify(routeRefreshService).refreshRoutes();
    }

    @Test
    void activeRouteDefinitionsMapDatabaseRoutesToSpringCloudGatewayRoutes() {
        GatewayRoute route = route(upstream("orders-api", "https://orders.example.com"));
        GatewayRouteDefinitionService service = new GatewayRouteDefinitionService(gatewayRouteRepository);
        when(gatewayRouteRepository.findRoutableRoutes()).thenReturn(List.of(route));

        List<RouteDefinition> definitions = service.activeRouteDefinitions();

        assertThat(definitions).hasSize(1);
        RouteDefinition definition = definitions.getFirst();
        assertThat(definition.getId()).isEqualTo("orders");
        assertThat(definition.getUri().toString()).isEqualTo("https://orders.example.com");
        assertThat(definition.getOrder()).isEqualTo(5);
        assertThat(definition.getPredicates()).hasSize(2);
        assertThat(definition.getPredicates()).extracting("name").containsExactly("Path", "Method");
        assertThat(definition.getFilters()).extracting("name").containsExactly("StripPrefix");
    }

    private GatewayRoute route(UpstreamService upstream) {
        GatewayRoute route = new GatewayRoute();
        ReflectionTestUtils.setField(route, "id", UUID.randomUUID());
        route.setUpstreamService(upstream);
        route.setRouteKey("orders");
        route.setName("Orders Route");
        route.setPathPattern("/proxy/orders/**");
        route.setAllowedMethods(new String[]{"GET", "POST"});
        route.setStripPrefix(1);
        route.setPriority(5);
        route.setStatus(GatewayRouteStatus.ACTIVE);
        route.replacePredicates(List.of(
                predicate(RoutePredicateType.PATH, Map.of("pattern", "/proxy/orders/**")),
                predicate(RoutePredicateType.METHOD, Map.of("methods", List.of("GET", "POST")))
        ));
        return route;
    }

    private GatewayRoutePredicate predicate(RoutePredicateType type, Map<String, Object> config) {
        GatewayRoutePredicate predicate = new GatewayRoutePredicate();
        ReflectionTestUtils.setField(predicate, "id", UUID.randomUUID());
        predicate.setPredicateType(type);
        predicate.setPredicateConfig(config);
        return predicate;
    }

    private UpstreamService upstream(String name, String baseUrl) {
        UpstreamService upstream = new UpstreamService();
        ReflectionTestUtils.setField(upstream, "id", UUID.randomUUID());
        upstream.setName(name);
        upstream.setBaseUrl(baseUrl);
        upstream.setStatus(UpstreamServiceStatus.ACTIVE);
        upstream.setTimeoutMs(5000);
        upstream.setRetryCount(1);
        return upstream;
    }
}
