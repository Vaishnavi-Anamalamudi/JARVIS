package com.adaptivegateway.operations.repository;

import com.adaptivegateway.operations.entity.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @EntityGraph(attributePaths = {"actorUser", "actorUser.role"})
    Page<AuditLog> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"actorUser", "actorUser.role"})
    Page<AuditLog> findByResourceTypeAndDeletedAtIsNull(String resourceType, Pageable pageable);
}
