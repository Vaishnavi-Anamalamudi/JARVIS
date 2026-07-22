package com.adaptivegateway.adaptive.repository;

import com.adaptivegateway.adaptive.entity.RateLimitAdjustment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimitAdjustmentRepository extends JpaRepository<RateLimitAdjustment, UUID> {

    @EntityGraph(attributePaths = {"policy", "route"})
    Page<RateLimitAdjustment> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"policy", "route"})
    Page<RateLimitAdjustment> findByPolicyIdAndDeletedAtIsNull(UUID policyId, Pageable pageable);
}
