package com.adaptivegateway.consumer.dto;

public record CreateCredentialResponse(
        ApiCredentialResponse credential,
        String apiKey
) {
}
