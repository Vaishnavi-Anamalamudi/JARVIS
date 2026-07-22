package com.adaptivegateway.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.ratelimit.dto.RateLimitPolicyRequest;
import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import com.adaptivegateway.ratelimit.mapper.RateLimitMapper;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitManagementServiceTest {

    @Mock
    private RateLimitPolicyRepository policyRepository;

    @Mock
    private RateLimitAssignmentRepository assignmentRepository;

    @Mock
    private GatewayRouteRepository routeRepository;

    private RateLimitManagementService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitManagementService(
                policyRepository,
                assignmentRepository,
                routeRepository,
                new RateLimitMapper()
        );
    }

    @Test
    void createPolicyRejectsSlidingWindowWithTokenBucketFields() {
        RateLimitPolicyRequest request = new RateLimitPolicyRequest(
                "Strict route window",
                RateLimitAlgorithm.SLIDING_WINDOW,
                60,
                100,
                100,
                10,
                1,
                BigDecimal.ONE,
                false,
                RateLimitPolicyStatus.ACTIVE
        );

        assertThatThrownBy(() -> service.createPolicy(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        verify(policyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createPolicyRejectsTokenBucketWithoutTokenBucketFields() {
        RateLimitPolicyRequest request = new RateLimitPolicyRequest(
                "Strict route bucket",
                RateLimitAlgorithm.TOKEN_BUCKET,
                60,
                100,
                null,
                null,
                null,
                BigDecimal.ONE,
                false,
                RateLimitPolicyStatus.ACTIVE
        );

        assertThatThrownBy(() -> service.createPolicy(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        verify(policyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
