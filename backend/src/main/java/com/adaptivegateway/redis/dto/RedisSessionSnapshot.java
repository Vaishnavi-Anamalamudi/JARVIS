package com.adaptivegateway.redis.dto;

import java.time.Instant;
import java.util.UUID;

public record RedisSessionSnapshot(
        UUID userId,
        String username,
        String email,
        String role,
        String status,
        Instant lastLoginAt,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        Instant cachedAt
) {
}
