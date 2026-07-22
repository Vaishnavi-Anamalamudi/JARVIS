package com.adaptivegateway.kafka.dto;

import java.time.Instant;

public record KafkaPublishSummaryResponse(
        int attempted,
        int published,
        int failed,
        Instant checkedAt
) {
}
