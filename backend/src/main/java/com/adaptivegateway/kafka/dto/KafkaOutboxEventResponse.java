package com.adaptivegateway.kafka.dto;

import com.adaptivegateway.kafka.enums.KafkaOutboxStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record KafkaOutboxEventResponse(
        UUID id,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        UUID correlationId,
        String sourceService,
        Integer schemaVersion,
        Map<String, Object> payload,
        KafkaOutboxStatus status,
        Integer attempts,
        Instant nextAttemptAt,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
