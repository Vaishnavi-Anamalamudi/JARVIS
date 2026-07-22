package com.adaptivegateway.anomaly.repository;

import com.adaptivegateway.anomaly.entity.AnomalyRecord;
import com.adaptivegateway.anomaly.enums.AnomalyStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyRecordRepository extends JpaRepository<AnomalyRecord, UUID> {

    @EntityGraph(attributePaths = {"statsSnapshot", "route"})
    Page<AnomalyRecord> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"statsSnapshot", "route"})
    Page<AnomalyRecord> findByStatusAndDeletedAtIsNull(AnomalyStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"statsSnapshot", "route"})
    Page<AnomalyRecord> findByRouteIdAndDeletedAtIsNull(UUID routeId, Pageable pageable);

    @EntityGraph(attributePaths = {"statsSnapshot", "route"})
    Page<AnomalyRecord> findByStatusAndRouteIdAndDeletedAtIsNull(AnomalyStatus status, UUID routeId, Pageable pageable);
}
