package com.adaptivegateway.anomaly.repository;

import com.adaptivegateway.anomaly.entity.AnomalyStatSnapshot;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyStatSnapshotRepository extends JpaRepository<AnomalyStatSnapshot, UUID> {

    @EntityGraph(attributePaths = {"route"})
    Page<AnomalyStatSnapshot> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"route"})
    Page<AnomalyStatSnapshot> findByRouteIdAndDeletedAtIsNull(UUID routeId, Pageable pageable);
}
