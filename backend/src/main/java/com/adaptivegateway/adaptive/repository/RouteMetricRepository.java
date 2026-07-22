package com.adaptivegateway.adaptive.repository;

import com.adaptivegateway.adaptive.entity.RouteMetric;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteMetricRepository extends JpaRepository<RouteMetric, UUID> {

    @EntityGraph(attributePaths = {"route"})
    Page<RouteMetric> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"route"})
    Optional<RouteMetric> findFirstByRouteIdAndWindowSecondsAndDeletedAtIsNullOrderByCalculatedAtDesc(
            UUID routeId,
            Integer windowSeconds
    );

    @EntityGraph(attributePaths = {"route"})
    Page<RouteMetric> findByRouteIdAndDeletedAtIsNull(UUID routeId, Pageable pageable);

    @EntityGraph(attributePaths = {"route"})
    List<RouteMetric> findByCalculatedAtGreaterThanEqualAndDeletedAtIsNullOrderByRouteIdAscCalculatedAtAsc(Instant calculatedAt);
}
