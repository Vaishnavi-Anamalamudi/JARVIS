package com.adaptivegateway.ratelimit.repository;

import com.adaptivegateway.ratelimit.entity.RequestLog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestLogRepository extends JpaRepository<RequestLog, UUID> {

    @EntityGraph(attributePaths = {"route", "route.upstreamService", "upstreamService"})
    Page<RequestLog> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"route", "route.upstreamService", "upstreamService"})
    Page<RequestLog> findByRouteIdAndDeletedAtIsNull(UUID routeId, Pageable pageable);

    @EntityGraph(attributePaths = {"route", "route.upstreamService", "upstreamService"})
    Page<RequestLog> findByGatewayOutcomeAndDeletedAtIsNull(com.adaptivegateway.ratelimit.enums.GatewayOutcome outcome, Pageable pageable);

    @EntityGraph(attributePaths = {"route", "route.upstreamService", "upstreamService"})
    Page<RequestLog> findByGatewayOutcomeAndRouteIdAndDeletedAtIsNull(
            com.adaptivegateway.ratelimit.enums.GatewayOutcome outcome,
            UUID routeId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"route", "route.upstreamService", "upstreamService"})
    Optional<RequestLog> findByIdAndDeletedAtIsNull(UUID id);

    @Query(value = """
            select
                route_id as routeId,
                count(*) as totalRequests,
                sum(case when gateway_outcome = 'ALLOWED' then 1 else 0 end) as allowedRequests,
                sum(case when gateway_outcome = 'BLOCKED' then 1 else 0 end) as blockedRequests,
                sum(case when gateway_outcome = 'ERROR' then 1 else 0 end) as errorRequests,
                avg(response_time_ms) as avgLatencyMs
            from request_logs
            where deleted_at is null
            and route_id is not null
            and started_at >= :windowStart
            and started_at < :windowEnd
            group by route_id
            """, nativeQuery = true)
    List<RouteTrafficAggregate> aggregateRouteTraffic(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    interface RouteTrafficAggregate {
        UUID getRouteId();

        long getTotalRequests();

        long getAllowedRequests();

        long getBlockedRequests();

        long getErrorRequests();

        BigDecimal getAvgLatencyMs();
    }
}
