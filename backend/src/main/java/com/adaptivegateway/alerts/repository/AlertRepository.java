package com.adaptivegateway.alerts.repository;

import com.adaptivegateway.alerts.entity.Alert;
import com.adaptivegateway.alerts.enums.AlertStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @EntityGraph(attributePaths = {"anomalyRecord", "route", "acknowledgedByUser"})
    java.util.Optional<Alert> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"anomalyRecord", "route", "acknowledgedByUser"})
    Page<Alert> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"anomalyRecord", "route", "acknowledgedByUser"})
    Page<Alert> findByStatusAndDeletedAtIsNull(AlertStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"anomalyRecord", "route", "acknowledgedByUser"})
    Page<Alert> findByRouteIdAndDeletedAtIsNull(UUID routeId, Pageable pageable);

    @EntityGraph(attributePaths = {"anomalyRecord", "route", "acknowledgedByUser"})
    Page<Alert> findByStatusAndRouteIdAndDeletedAtIsNull(AlertStatus status, UUID routeId, Pageable pageable);
}
