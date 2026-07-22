package com.adaptivegateway.gateway.repository;

import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.enums.GatewayRouteStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GatewayRouteRepository extends JpaRepository<GatewayRoute, UUID> {

    @EntityGraph(attributePaths = {"upstreamService", "predicates"})
    Page<GatewayRoute> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"upstreamService", "predicates"})
    Page<GatewayRoute> findByStatusAndDeletedAtIsNull(GatewayRouteStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"upstreamService", "predicates"})
    Optional<GatewayRoute> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"upstreamService", "predicates"})
    Optional<GatewayRoute> findByRouteKeyIgnoreCaseAndDeletedAtIsNull(String routeKey);

    boolean existsByRouteKeyIgnoreCaseAndDeletedAtIsNull(String routeKey);

    @EntityGraph(attributePaths = {"upstreamService", "predicates"})
    @Query("""
            select route from GatewayRoute route
            where route.deletedAt is null
            and route.status = com.adaptivegateway.gateway.enums.GatewayRouteStatus.ACTIVE
            and route.upstreamService.deletedAt is null
            and route.upstreamService.status in (
                com.adaptivegateway.gateway.enums.UpstreamServiceStatus.ACTIVE,
                com.adaptivegateway.gateway.enums.UpstreamServiceStatus.DEGRADED
            )
            order by route.priority asc, route.createdAt asc
            """)
    List<GatewayRoute> findRoutableRoutes();
}
