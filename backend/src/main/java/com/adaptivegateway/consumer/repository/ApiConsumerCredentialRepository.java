package com.adaptivegateway.consumer.repository;

import com.adaptivegateway.consumer.entity.ApiConsumerCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiConsumerCredentialRepository extends JpaRepository<ApiConsumerCredential, UUID> {

    @EntityGraph(attributePaths = {"consumer", "consumer.ownerUser"})
    Page<ApiConsumerCredential> findByConsumerIdAndDeletedAtIsNull(UUID consumerId, Pageable pageable);

    @EntityGraph(attributePaths = {"consumer", "consumer.ownerUser"})
    Optional<ApiConsumerCredential> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByKeyPrefixAndDeletedAtIsNull(String keyPrefix);
}
