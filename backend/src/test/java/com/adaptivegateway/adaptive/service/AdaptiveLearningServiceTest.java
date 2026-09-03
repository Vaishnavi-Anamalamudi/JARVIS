package com.adaptivegateway.adaptive.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.adaptive.config.AdaptiveLearningProperties;
import com.adaptivegateway.adaptive.entity.RateLimitAdjustment;
import com.adaptivegateway.adaptive.entity.RouteMetric;
import com.adaptivegateway.adaptive.entity.TrafficHeatmapBucket;
import com.adaptivegateway.adaptive.repository.RateLimitAdjustmentRepository;
import com.adaptivegateway.adaptive.repository.RouteMetricRepository;
import com.adaptivegateway.adaptive.repository.TrafficHeatmapBucketRepository;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitPolicyStatus;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import com.adaptivegateway.ratelimit.repository.RequestLogRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdaptiveLearningServiceTest {

    @Mock
    private RequestLogRepository requestLogRepository;

    @Mock
    private GatewayRouteRepository routeRepository;

    @Mock
    private RouteMetricRepository routeMetricRepository;

    @Mock
    private TrafficHeatmapBucketRepository heatmapRepository;

    @Mock
    private RateLimitAssignmentRepository assignmentRepository;

    @Mock
    private RateLimitPolicyRepository policyRepository;

    @Mock
    private RateLimitAdjustmentRepository adjustmentRepository;

    @Mock
    private KafkaEventPublisherService kafkaEventPublisherService;

    @Mock
    private LiveEventService liveEventService;

    private AdaptiveLearningService service;

    @BeforeEach
    void setUp() {
        service = new AdaptiveLearningService(
                new AdaptiveLearningProperties(
                        300,
                        60000,
                        10,
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("0.1000"),
                        new BigDecimal("0.1000"),
                        new BigDecimal("10.0000")
                ),
                requestLogRepository,
                routeRepository,
                routeMetricRepository,
                heatmapRepository,
                assignmentRepository,
                policyRepository,
                adjustmentRepository,
                kafkaEventPublisherService,
                liveEventService
        );
    }

    @Test
    void runLearningCycleWritesMetricHeatmapAndRelaxesStrictnessWhenBlockedRateIsHighAndErrorsLow() {
        UUID routeId = UUID.randomUUID();
        GatewayRoute route = route(routeId);
        RateLimitPolicy policy = policy(new BigDecimal("1.0000"));
        RateLimitAssignment assignment = new RateLimitAssignment();
        assignment.setPolicy(policy);
        assignment.setRoute(route);

        when(requestLogRepository.aggregateRouteTraffic(any(), any())).thenReturn(List.of(aggregate(routeId, 100, 40, 0)));
        when(routeRepository.getReferenceById(routeId)).thenReturn(route);
        when(heatmapRepository.findByBucketStartAndRouteIdAndConsumerIdIsNullAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.findActiveAdaptiveRouteAssignments(any())).thenReturn(List.of(assignment));
        when(adjustmentRepository.save(any(RateLimitAdjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.runLearningCycle();

        assertThat(response.routeMetricsWritten()).isEqualTo(1);
        assertThat(response.heatmapBucketsWritten()).isEqualTo(1);
        assertThat(response.strictnessAdjustmentsWritten()).isEqualTo(1);
        assertThat(policy.getStrictnessFactor()).isEqualByComparingTo("0.9000");
        verify(routeMetricRepository).save(any(RouteMetric.class));
        verify(heatmapRepository).save(any(TrafficHeatmapBucket.class));
        verify(policyRepository).save(policy);
    }

    private static RequestLogRepository.RouteTrafficAggregate aggregate(UUID routeId, long total, long blocked, long errors) {
        return new RequestLogRepository.RouteTrafficAggregate() {
            @Override
            public UUID getRouteId() {
                return routeId;
            }

            @Override
            public long getTotalRequests() {
                return total;
            }

            @Override
            public long getAllowedRequests() {
                return total - blocked - errors;
            }

            @Override
            public long getBlockedRequests() {
                return blocked;
            }

            @Override
            public long getErrorRequests() {
                return errors;
            }

            @Override
            public BigDecimal getAvgLatencyMs() {
                return new BigDecimal("25.0000");
            }
        };
    }

    private static GatewayRoute route(UUID routeId) {
        GatewayRoute route = new GatewayRoute();
        ReflectionTestUtils.setField(route, "id", routeId);
        route.setRouteKey("orders");
        route.setName("Orders");
        return route;
    }

    private static RateLimitPolicy policy(BigDecimal strictness) {
        RateLimitPolicy policy = new RateLimitPolicy();
        ReflectionTestUtils.setField(policy, "id", UUID.randomUUID());
        policy.setName("Adaptive orders");
        policy.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        policy.setWindowSeconds(60);
        policy.setMaxRequests(100);
        policy.setStrictnessFactor(strictness);
        policy.setAdaptiveEnabled(true);
        policy.setStatus(RateLimitPolicyStatus.ACTIVE);
        return policy;
    }
}
