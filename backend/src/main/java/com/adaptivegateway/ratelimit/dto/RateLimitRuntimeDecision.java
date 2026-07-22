package com.adaptivegateway.ratelimit.dto;

import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitDecisionValue;
import java.math.BigDecimal;
import java.util.UUID;

public record RateLimitRuntimeDecision(
        UUID policyId,
        UUID assignmentId,
        RateLimitAlgorithm algorithm,
        RateLimitDecisionValue decision,
        int effectiveLimit,
        Integer observedCount,
        Integer remainingTokens,
        int retryAfterSeconds,
        BigDecimal strictnessFactor
) {

    public boolean allowed() {
        return decision == RateLimitDecisionValue.ALLOW;
    }
}
