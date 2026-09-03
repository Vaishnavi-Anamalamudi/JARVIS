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

    @Query(value = """
            select
                consumer_id as consumerId,
                route_id as routeId,
                count(*) as totalRequests,
                sum(case when gateway_outcome = 'ALLOWED' then 1 else 0 end) as allowedRequests,
                sum(case when gateway_outcome = 'BLOCKED' then 1 else 0 end) as blockedRequests,
                sum(case when gateway_outcome = 'ERROR' then 1 else 0 end) as errorRequests,
                avg(response_time_ms) as avgResponseTimeMs,
                percentile_cont(0.95) within group (order by response_time_ms) as p95ResponseTimeMs,
                count(distinct source_ip) as uniqueSourceIps
            from request_logs
            where deleted_at is null
            and started_at >= :bucketStart
            and started_at < :bucketEnd
            group by grouping sets ((), (route_id), (consumer_id), (route_id, consumer_id))
            """, nativeQuery = true)
    List<AnalyticsTrafficAggregate> aggregateAnalyticsRollups(
            @Param("bucketStart") Instant bucketStart,
            @Param("bucketEnd") Instant bucketEnd
    );

    @Query(value = """
            select
                consumer_id as consumerId,
                count(*) as totalRequests,
                sum(case when gateway_outcome = 'ALLOWED' then 1 else 0 end) as allowedRequests,
                sum(case when gateway_outcome = 'BLOCKED' then 1 else 0 end) as blockedRequests,
                sum(case when gateway_outcome = 'ERROR' then 1 else 0 end) as errorRequests,
                avg(response_time_ms) as avgLatencyMs
            from request_logs
            where deleted_at is null
            and consumer_id is not null
            and started_at >= :windowStart
            and started_at < :windowEnd
            group by consumer_id
            """, nativeQuery = true)
    List<ClientTrafficAggregate> aggregateClientTraffic(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    @Query(value = """
            select
                count(*) as totalRequests,
                coalesce(sum(case when gateway_outcome = 'ALLOWED' then 1 else 0 end), 0) as allowedRequests,
                coalesce(sum(case when gateway_outcome = 'BLOCKED' then 1 else 0 end), 0) as blockedRequests,
                coalesce(sum(case when gateway_outcome = 'ERROR' then 1 else 0 end), 0) as errorRequests,
                avg(response_time_ms) as avgResponseTimeMs,
                percentile_cont(0.95) within group (order by response_time_ms) as p95ResponseTimeMs,
                count(distinct source_ip) as uniqueSourceIps
            from request_logs
            where deleted_at is null
            and started_at >= :windowStart
            and started_at < :windowEnd
            """, nativeQuery = true)
    AnalyticsSummaryAggregate summarizeTraffic(
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

    interface AnalyticsTrafficAggregate {
        UUID getConsumerId();

        UUID getRouteId();

        long getTotalRequests();

        long getAllowedRequests();

        long getBlockedRequests();

        long getErrorRequests();

        BigDecimal getAvgResponseTimeMs();

        BigDecimal getP95ResponseTimeMs();

        int getUniqueSourceIps();
    }

    interface ClientTrafficAggregate {
        UUID getConsumerId();

        long getTotalRequests();

        long getBlockedRequests();

        long getErrorRequests();

        BigDecimal getAvgLatencyMs();
    }

    interface AnalyticsSummaryAggregate {
        long getTotalRequests();

        long getAllowedRequests();

        long getBlockedRequests();

        long getErrorRequests();

        BigDecimal getAvgResponseTimeMs();

        BigDecimal getP95ResponseTimeMs();

        int getUniqueSourceIps();
    }
}
