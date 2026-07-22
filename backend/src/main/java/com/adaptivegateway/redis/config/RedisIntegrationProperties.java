package com.adaptivegateway.redis.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "adaptive-gateway.redis")
public record RedisIntegrationProperties(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9:_-]+$") String keyPrefix,
        @Min(1) long sessionCacheTtlMinutes,
        @Min(1) long temporaryStatsTtlMinutes
) {

    public Duration sessionCacheTtl() {
        return Duration.ofMinutes(sessionCacheTtlMinutes);
    }

    public Duration temporaryStatsTtl() {
        return Duration.ofMinutes(temporaryStatsTtlMinutes);
    }
}
