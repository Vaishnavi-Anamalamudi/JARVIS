package com.adaptivegateway.ratelimit.dto;

import com.adaptivegateway.gateway.entity.GatewayRoute;
import java.util.Optional;

public record RateLimitEvaluation(
        GatewayRoute route,
        RateLimitRuntimeDecision decision
) {

    public boolean limited() {
        return decision != null;
    }

    public boolean allowed() {
        return decision == null || decision.allowed();
    }

    public Optional<RateLimitRuntimeDecision> decisionOptional() {
        return Optional.ofNullable(decision);
    }
}
