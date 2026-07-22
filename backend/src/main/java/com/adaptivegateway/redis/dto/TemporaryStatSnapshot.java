package com.adaptivegateway.redis.dto;

import java.time.Instant;

public record TemporaryStatSnapshot(
        String key,
        long value,
        long ttlSeconds,
        Instant readAt
) {
}
