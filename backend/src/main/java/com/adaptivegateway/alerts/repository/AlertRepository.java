package com.adaptivegateway.alerts.repository;

import com.adaptivegateway.alerts.entity.Alert;
import com.adaptivegateway.alerts.enums.AlertStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @EntityGraph(attributePaths = {"anomalyRecord", "route"})
    Page<Alert> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"anomalyRecord", "route"})
    Page<Alert> findByStatusAndDeletedAtIsNull(AlertStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"anomalyRecord", "route"})
    Page<Alert> findByRouteIdAndDeletedAtIsNull(UUID routeId, Pageable pageable);

    @EntityGraph(attributePaths = {"anomalyRecord", "route"})
    Page<Alert> findByStatusAndRouteIdAndDeletedAtIsNull(AlertStatus status, UUID routeId, Pageable pageable);
}
