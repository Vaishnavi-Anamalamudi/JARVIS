package com.adaptivegateway.consumer.dto;

import com.adaptivegateway.consumer.enums.ApiConsumerEnvironment;
import com.adaptivegateway.consumer.enums.ApiConsumerStatus;
import java.time.Instant;
import java.util.UUID;

public record ApiConsumerResponse(
        UUID id,
        UUID ownerUserId,
        String ownerUsername,
        String name,
        String description,
        String contactEmail,
        ApiConsumerStatus status,
        ApiConsumerEnvironment environment,
        Instant createdAt,
        Instant updatedAt
) {
}
