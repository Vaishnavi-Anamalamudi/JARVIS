package com.adaptivegateway.ratelimit.dto;

import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RateLimitPolicyResponse(
        UUID id,
        String name,
        RateLimitAlgorithm algorithm,
        Integer windowSeconds,
        Integer maxRequests,
        Integer bucketCapacity,
        Integer refillTokens,
        Integer refillPeriodSeconds,
        BigDecimal strictnessFactor,
        Boolean adaptiveEnabled,
        RateLimitPolicyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
