package com.adaptivegateway.anomaly.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "adaptive-gateway.anomaly-detection")
public record AnomalyDetectionProperties(
        @Min(1) long lookbackMinutes,
        @Min(2) int minimumSamples,
        @DecimalMin("0.1000") BigDecimal zScoreThreshold,
        @DecimalMin("0.1000") BigDecimal thresholdStddevMultiplier,
        @DecimalMin("0.0001") @DecimalMax("1.0000") BigDecimal emaAlpha,
        @Min(1000) long detectionIntervalMs,
        @DecimalMin("0.0001") @DecimalMax("2.0000") BigDecimal strictnessIncreaseStep,
        @DecimalMin("0.1000") @DecimalMax("10.0000") BigDecimal maxStrictnessFactor
) {
}
