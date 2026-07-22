package com.adaptivegateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "adaptive-gateway")
public record ApplicationProperties(
        @NotBlank String serviceName,
        @NotBlank String apiVersion
) {
}
