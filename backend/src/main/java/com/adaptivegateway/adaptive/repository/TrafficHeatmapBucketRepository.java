package com.adaptivegateway.adaptive.repository;

import com.adaptivegateway.adaptive.entity.TrafficHeatmapBucket;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrafficHeatmapBucketRepository extends JpaRepository<TrafficHeatmapBucket, UUID> {

    @EntityGraph(attributePaths = {"route"})
    Page<TrafficHeatmapBucket> findByDeletedAtIsNull(Pageable pageable);

    Optional<TrafficHeatmapBucket> findByBucketStartAndRouteIdAndConsumerIdIsNullAndDeletedAtIsNull(
            Instant bucketStart,
            UUID routeId
    );
}
