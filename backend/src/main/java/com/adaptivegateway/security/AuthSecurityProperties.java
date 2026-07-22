package com.adaptivegateway.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "adaptive-gateway.auth")
public record AuthSecurityProperties(
        @NotBlank String adminRoleName,
        @Valid Jwt jwt
) {

    public record Jwt(
            @NotBlank String issuer,
            @NotBlank String secretBase64,
            @Min(1) long accessTokenTtlMinutes,
            @Min(1) long refreshTokenTtlDays
    ) {

        public Duration accessTokenTtl() {
            return Duration.ofMinutes(accessTokenTtlMinutes);
        }

        public Duration refreshTokenTtl() {
            return Duration.ofDays(refreshTokenTtlDays);
        }
    }
}
