package com.adaptivegateway.kafka.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record KafkaHealthResponse(
        @Schema(example = "UP") String status,
        String healthTopic,
        long roundTripMillis,
        Instant checkedAt
) {
}
