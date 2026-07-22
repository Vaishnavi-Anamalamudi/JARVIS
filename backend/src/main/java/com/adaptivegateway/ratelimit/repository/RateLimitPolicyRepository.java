package com.adaptivegateway.ratelimit.repository;

import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicy, UUID> {

    Page<RateLimitPolicy> findByDeletedAtIsNull(Pageable pageable);

    Page<RateLimitPolicy> findByStatusAndDeletedAtIsNull(RateLimitPolicyStatus status, Pageable pageable);

    Optional<RateLimitPolicy> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
