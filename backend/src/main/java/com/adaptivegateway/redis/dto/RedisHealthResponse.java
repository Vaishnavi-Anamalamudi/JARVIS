package com.adaptivegateway.redis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record RedisHealthResponse(
        @Schema(example = "UP") String status,
        String keyPrefix,
        long roundTripMillis,
        Instant checkedAt
) {
}
