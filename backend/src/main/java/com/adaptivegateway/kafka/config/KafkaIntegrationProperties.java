package com.adaptivegateway.kafka.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "adaptive-gateway.kafka")
public record KafkaIntegrationProperties(
        @NotBlank String defaultTopic,
        @NotBlank String requestEventsTopic,
        @NotBlank String healthTopic,
        @Min(1) int eventSchemaVersion,
        @Min(100) long publishTimeoutMs,
        @Min(1) long retryDelaySeconds,
        @Min(1) @Max(100) int maxAttempts,
        @Min(1) @Max(500) int outboxBatchSize,
        @Min(1000) long outboxPollIntervalMs
) {

    public Duration publishTimeout() {
        return Duration.ofMillis(publishTimeoutMs);
    }

    public Duration retryDelay() {
        return Duration.ofSeconds(retryDelaySeconds);
    }
}
