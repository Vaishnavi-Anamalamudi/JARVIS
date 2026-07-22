package com.adaptivegateway.ratelimit.mapper;

import com.adaptivegateway.ratelimit.dto.RateLimitAssignmentResponse;
import com.adaptivegateway.ratelimit.dto.RateLimitPolicyResponse;
import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import org.springframework.stereotype.Component;

@Component
public class RateLimitMapper {

    public RateLimitPolicyResponse toResponse(RateLimitPolicy policy) {
        return new RateLimitPolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getAlgorithm(),
                policy.getWindowSeconds(),
                policy.getMaxRequests(),
                policy.getBucketCapacity(),
                policy.getRefillTokens(),
                policy.getRefillPeriodSeconds(),
                policy.getStrictnessFactor(),
                policy.getAdaptiveEnabled(),
                policy.getStatus(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    public RateLimitAssignmentResponse toResponse(RateLimitAssignment assignment) {
        return new RateLimitAssignmentResponse(
                assignment.getId(),
                assignment.getPolicy().getId(),
                assignment.getPolicy().getName(),
                assignment.getRoute().getId(),
                assignment.getRoute().getRouteKey(),
                assignment.getRoute().getName(),
                assignment.getPriority(),
                assignment.getStatus(),
                assignment.getValidFrom(),
                assignment.getValidUntil(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
