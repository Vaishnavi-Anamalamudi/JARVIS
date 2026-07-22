package com.adaptivegateway.ratelimit.dto;

public record RedisRateLimitResult(
        boolean allowed,
        int observedCount,
        int remainingTokens,
        int retryAfterSeconds
) {
}
