package com.adaptivegateway.ratelimit.repository;

import com.adaptivegateway.ratelimit.entity.RateLimitDecision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimitDecisionRepository extends JpaRepository<RateLimitDecision, UUID> {

    Optional<RateLimitDecision> findByRequestLogIdAndDeletedAtIsNull(UUID requestLogId);
}
