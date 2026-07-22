package com.adaptivegateway.ratelimit.dto;

import com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record RateLimitAssignmentRequest(
        @NotNull UUID policyId,
        @NotNull UUID routeId,
        @NotNull @Min(0) Integer priority,
        @NotNull RateLimitAssignmentStatus status,
        @NotNull Instant validFrom,
        Instant validUntil
) {
}
