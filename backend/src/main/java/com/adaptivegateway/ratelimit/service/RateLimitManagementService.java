package com.adaptivegateway.ratelimit.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.ratelimit.dto.RateLimitAssignmentRequest;
import com.adaptivegateway.ratelimit.dto.RateLimitAssignmentResponse;
import com.adaptivegateway.ratelimit.dto.RateLimitPolicyRequest;
import com.adaptivegateway.ratelimit.dto.RateLimitPolicyResponse;
import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitAssignmentStatus;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import com.adaptivegateway.ratelimit.mapper.RateLimitMapper;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitManagementService {

    private static final List<String> POLICY_SORTS = List.of("createdAt", "updatedAt", "name", "algorithm", "status");
    private static final List<String> ASSIGNMENT_SORTS = List.of("createdAt", "updatedAt", "priority", "status", "validFrom");

    private final RateLimitPolicyRepository policyRepository;
    private final RateLimitAssignmentRepository assignmentRepository;
    private final GatewayRouteRepository routeRepository;
    private final RateLimitMapper mapper;

    public RateLimitManagementService(
            RateLimitPolicyRepository policyRepository,
            RateLimitAssignmentRepository assignmentRepository,
            GatewayRouteRepository routeRepository,
            RateLimitMapper mapper
    ) {
        this.policyRepository = policyRepository;
        this.assignmentRepository = assignmentRepository;
        this.routeRepository = routeRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RateLimitPolicyResponse> listPolicies(
            RateLimitPolicyStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, POLICY_SORTS, "createdAt");
        var policies = status == null
                ? policyRepository.findByDeletedAtIsNull(pageRequest)
                : policyRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        return PageResponse.from(policies.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public RateLimitPolicyResponse getPolicy(UUID id) {
        return mapper.toResponse(findPolicy(id));
    }

    @Transactional
    public RateLimitPolicyResponse createPolicy(RateLimitPolicyRequest request) {
        validatePolicy(request);
        String name = request.name().trim();
        if (policyRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Rate limit policy name is already registered");
        }
        RateLimitPolicy policy = new RateLimitPolicy();
        apply(request, policy);
        return mapper.toResponse(policyRepository.save(policy));
    }

    @Transactional
    public RateLimitPolicyResponse updatePolicy(UUID id, RateLimitPolicyRequest request) {
        validatePolicy(request);
        RateLimitPolicy policy = findPolicy(id);
        String name = request.name().trim();
        if (!policy.getName().equalsIgnoreCase(name) && policyRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Rate limit policy name is already registered");
        }
        apply(request, policy);
        return mapper.toResponse(policyRepository.save(policy));
    }

    @Transactional
    public void deletePolicy(UUID id) {
        RateLimitPolicy policy = findPolicy(id);
        policy.setDeletedAt(Instant.now());
        policyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public PageResponse<RateLimitAssignmentResponse> listAssignments(
            RateLimitAssignmentStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, ASSIGNMENT_SORTS, "priority");
        var assignments = status == null
                ? assignmentRepository.findByDeletedAtIsNull(pageRequest)
                : assignmentRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        return PageResponse.from(assignments.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public RateLimitAssignmentResponse getAssignment(UUID id) {
        return mapper.toResponse(findAssignment(id));
    }

    @Transactional
    public RateLimitAssignmentResponse createAssignment(RateLimitAssignmentRequest request) {
        validateAssignment(request);
        RateLimitAssignment assignment = new RateLimitAssignment();
        apply(request, assignment);
        return mapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public RateLimitAssignmentResponse updateAssignment(UUID id, RateLimitAssignmentRequest request) {
        validateAssignment(request);
        RateLimitAssignment assignment = findAssignment(id);
        apply(request, assignment);
        return mapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(UUID id) {
        RateLimitAssignment assignment = findAssignment(id);
        assignment.setDeletedAt(Instant.now());
        assignmentRepository.save(assignment);
    }

    private void apply(RateLimitPolicyRequest request, RateLimitPolicy policy) {
        policy.setName(request.name().trim());
        policy.setAlgorithm(request.algorithm());
        policy.setWindowSeconds(request.windowSeconds());
        policy.setMaxRequests(request.maxRequests());
        policy.setBucketCapacity(request.bucketCapacity());
        policy.setRefillTokens(request.refillTokens());
        policy.setRefillPeriodSeconds(request.refillPeriodSeconds());
        policy.setStrictnessFactor(request.strictnessFactor());
        policy.setAdaptiveEnabled(request.adaptiveEnabled());
        policy.setStatus(request.status());
    }

    private void apply(RateLimitAssignmentRequest request, RateLimitAssignment assignment) {
        assignment.setPolicy(findPolicy(request.policyId()));
        assignment.setRoute(findRoute(request.routeId()));
        assignment.setConsumerId(null);
        assignment.setPriority(request.priority());
        assignment.setStatus(request.status());
        assignment.setValidFrom(request.validFrom());
        assignment.setValidUntil(request.validUntil());
    }

    private void validatePolicy(RateLimitPolicyRequest request) {
        boolean bucketFieldsPresent = request.bucketCapacity() != null
                && request.refillTokens() != null
                && request.refillPeriodSeconds() != null;
        if (request.algorithm() == RateLimitAlgorithm.SLIDING_WINDOW && bucketFieldsPresent) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sliding window policies must not define token bucket settings");
        }
        if (request.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW && !bucketFieldsPresent) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Token bucket settings are required for this policy algorithm");
        }
    }

    private void validateAssignment(RateLimitAssignmentRequest request) {
        if (request.validUntil() != null && !request.validUntil().isAfter(request.validFrom())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Assignment validUntil must be after validFrom");
        }
    }

    private RateLimitPolicy findPolicy(UUID id) {
        return policyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Rate limit policy does not exist"));
    }

    private RateLimitAssignment findAssignment(UUID id) {
        return assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Rate limit assignment does not exist"));
    }

    private GatewayRoute findRoute(UUID id) {
        return routeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Gateway route does not exist"));
    }

    private PageRequest pageRequest(
            int page,
            int size,
            String sort,
            String direction,
            List<String> allowedSorts,
            String defaultSort
    ) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
