package com.adaptivegateway.foundation.dto;

import com.adaptivegateway.redis.dto.RedisHealthResponse;
import com.adaptivegateway.kafka.dto.KafkaHealthResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SystemHealthResponse(
        String serviceName,
        String apiVersion,
        @Schema(example = "UP") String status,
        DatabaseHealthResponse database,
        RedisHealthResponse redis,
        KafkaHealthResponse kafka,
        Instant checkedAt
) {
}
