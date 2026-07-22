package com.adaptivegateway.consumer.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiCredentialResponse(
        UUID id,
        UUID consumerId,
        String keyPrefix,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
