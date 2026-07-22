package com.adaptivegateway.adaptive.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "adaptive-gateway.adaptive-learning")
public record AdaptiveLearningProperties(
        @Min(60) int metricWindowSeconds,
        @Min(1000) long aggregationIntervalMs,
        @Min(1) long minimumRequestsForAdjustment,
        @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal blockedRateRelaxThreshold,
        @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal errorRateTightenThreshold,
        @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal maxErrorRateForRelaxation,
        @DecimalMin("0.0001") @DecimalMax("2.0000") BigDecimal strictnessAdjustmentStep,
        @DecimalMin("0.1000") @DecimalMax("10.0000") BigDecimal minStrictnessFactor,
        @DecimalMin("0.1000") @DecimalMax("10.0000") BigDecimal maxStrictnessFactor
) {
}
