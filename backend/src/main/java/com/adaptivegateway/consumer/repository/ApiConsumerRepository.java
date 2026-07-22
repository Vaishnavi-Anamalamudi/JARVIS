package com.adaptivegateway.consumer.repository;

import com.adaptivegateway.consumer.entity.ApiConsumer;
import com.adaptivegateway.consumer.enums.ApiConsumerStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiConsumerRepository extends JpaRepository<ApiConsumer, UUID> {

    @EntityGraph(attributePaths = {"ownerUser", "ownerUser.role"})
    Page<ApiConsumer> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"ownerUser", "ownerUser.role"})
    Page<ApiConsumer> findByStatusAndDeletedAtIsNull(ApiConsumerStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"ownerUser", "ownerUser.role"})
    Optional<ApiConsumer> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOwnerUserIdAndNameIgnoreCaseAndEnvironmentAndDeletedAtIsNull(
            UUID ownerUserId,
            String name,
            com.adaptivegateway.consumer.enums.ApiConsumerEnvironment environment
    );
}
