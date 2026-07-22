package com.adaptivegateway.kafka.dto;

import java.util.Map;
import java.util.UUID;

public record KafkaEventRequest(
        String topic,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String correlationId,
        Map<String, Object> payload
) {
}
