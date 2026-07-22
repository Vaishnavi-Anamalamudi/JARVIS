package com.adaptivegateway.consumer.dto;

import java.time.Instant;

public record CreateCredentialRequest(
        Instant expiresAt
) {
}
