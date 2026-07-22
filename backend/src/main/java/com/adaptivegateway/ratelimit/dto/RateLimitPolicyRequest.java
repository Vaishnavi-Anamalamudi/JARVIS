package com.adaptivegateway.ratelimit.dto;

import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RateLimitPolicyRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull RateLimitAlgorithm algorithm,
        @NotNull @Min(1) Integer windowSeconds,
        @NotNull @Min(1) Integer maxRequests,
        @Min(1) Integer bucketCapacity,
        @Min(1) Integer refillTokens,
        @Min(1) Integer refillPeriodSeconds,
        @NotNull @DecimalMin("0.1000") @DecimalMax("10.0000") BigDecimal strictnessFactor,
        @NotNull Boolean adaptiveEnabled,
        @NotNull RateLimitPolicyStatus status
) {
}
