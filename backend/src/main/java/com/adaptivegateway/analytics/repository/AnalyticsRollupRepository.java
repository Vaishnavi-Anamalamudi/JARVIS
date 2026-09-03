package com.adaptivegateway.analytics.repository;

import com.adaptivegateway.analytics.entity.AnalyticsRollup;
import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsRollupRepository extends JpaRepository<AnalyticsRollup, UUID> {

    @EntityGraph(attributePaths = {"consumer", "route"})
    @Query("""
            select rollup
            from AnalyticsRollup rollup
            where rollup.deletedAt is null
            and (:granularity is null or rollup.granularity = :granularity)
            and (:consumerId is null or rollup.consumer.id = :consumerId)
            and (:routeId is null or rollup.route.id = :routeId)
            """)
    Page<AnalyticsRollup> search(
            @Param("granularity") AnalyticsGranularity granularity,
            @Param("consumerId") UUID consumerId,
            @Param("routeId") UUID routeId,
            Pageable pageable
    );

    @Query("""
            select rollup
            from AnalyticsRollup rollup
            where rollup.deletedAt is null
            and rollup.bucketStart = :bucketStart
            and rollup.bucketEnd = :bucketEnd
            and rollup.granularity = :granularity
            and ((:consumerId is null and rollup.consumer is null) or rollup.consumer.id = :consumerId)
            and ((:routeId is null and rollup.route is null) or rollup.route.id = :routeId)
            """)
    Optional<AnalyticsRollup> findExisting(
            @Param("bucketStart") Instant bucketStart,
            @Param("bucketEnd") Instant bucketEnd,
            @Param("granularity") AnalyticsGranularity granularity,
            @Param("consumerId") UUID consumerId,
            @Param("routeId") UUID routeId
    );
}
