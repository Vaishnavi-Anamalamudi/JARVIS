package com.adaptivegateway.gateway.repository;

import com.adaptivegateway.gateway.entity.UpstreamService;
import com.adaptivegateway.gateway.enums.UpstreamServiceStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpstreamServiceRepository extends JpaRepository<UpstreamService, UUID> {

    Page<UpstreamService> findByDeletedAtIsNull(Pageable pageable);

    Page<UpstreamService> findByStatusAndDeletedAtIsNull(UpstreamServiceStatus status, Pageable pageable);

    Optional<UpstreamService> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
