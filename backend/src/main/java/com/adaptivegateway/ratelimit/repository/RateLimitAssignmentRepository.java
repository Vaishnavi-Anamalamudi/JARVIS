package com.adaptivegateway.ratelimit.repository;

import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RateLimitAssignmentRepository extends JpaRepository<RateLimitAssignment, UUID> {

    @EntityGraph(attributePaths = {"policy", "route", "route.upstreamService"})
    Page<RateLimitAssignment> findByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"policy", "route", "route.upstreamService"})
    Page<RateLimitAssignment> findByStatusAndDeletedAtIsNull(RateLimitAssignmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"policy", "route", "route.upstreamService"})
    Optional<RateLimitAssignment> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"policy", "route", "route.upstreamService"})
    @Query("""
            select assignment from RateLimitAssignment assignment
            where assignment.deletedAt is null
            and assignment.status = com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus.ACTIVE
            and assignment.policy.deletedAt is null
            and assignment.policy.status = com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus.ACTIVE
            and assignment.route.id = :routeId
            and assignment.consumerId is null
            and assignment.validFrom <= :now
            and (assignment.validUntil is null or assignment.validUntil > :now)
            order by assignment.priority asc, assignment.createdAt asc
            """)
    List<RateLimitAssignment> findActiveRouteAssignments(@Param("routeId") UUID routeId, @Param("now") Instant now, Pageable pageable);

    @EntityGraph(attributePaths = {"policy", "route", "route.upstreamService"})
    @Query("""
            select assignment from RateLimitAssignment assignment
            where assignment.deletedAt is null
            and assignment.status = com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus.ACTIVE
            and assignment.policy.deletedAt is null
            and assignment.policy.status = com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus.ACTIVE
            and assignment.policy.adaptiveEnabled = true
            and assignment.route is not null
            and assignment.validFrom <= :now
            and (assignment.validUntil is null or assignment.validUntil > :now)
            order by assignment.priority asc, assignment.createdAt asc
            """)
    List<RateLimitAssignment> findActiveAdaptiveRouteAssignments(@Param("now") Instant now);
}
