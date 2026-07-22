package com.adaptivegateway.operations.dto;

import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitDecisionValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RateLimitDecisionSnapshotResponse(
        UUID id,
        UUID policyId,
        UUID assignmentId,
        RateLimitAlgorithm algorithm,
        RateLimitDecisionValue decision,
        Integer effectiveLimit,
        Integer observedCount,
        Integer remainingTokens,
        Integer retryAfterSeconds,
        BigDecimal strictnessFactor,
        Instant createdAt
) {
}
