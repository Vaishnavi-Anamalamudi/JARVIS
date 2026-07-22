package com.adaptivegateway.anomaly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.adaptive.entity.RouteMetric;
import com.adaptivegateway.adaptive.repository.RateLimitAdjustmentRepository;
import com.adaptivegateway.adaptive.repository.RouteMetricRepository;
import com.adaptivegateway.alerts.entity.Alert;
import com.adaptivegateway.alerts.repository.AlertRepository;
import com.adaptivegateway.anomaly.config.AnomalyDetectionProperties;
import com.adaptivegateway.anomaly.entity.AnomalyRecord;
import com.adaptivegateway.anomaly.entity.AnomalyStatSnapshot;
import com.adaptivegateway.anomaly.mapper.AnomalyDetectionMapper;
import com.adaptivegateway.anomaly.repository.AnomalyRecordRepository;
import com.adaptivegateway.anomaly.repository.AnomalyStatSnapshotRepository;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private RouteMetricRepository routeMetricRepository;

    @Mock
    private AnomalyStatSnapshotRepository snapshotRepository;

    @Mock
    private AnomalyRecordRepository anomalyRecordRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RateLimitAssignmentRepository assignmentRepository;

    @Mock
    private RateLimitPolicyRepository policyRepository;

    @Mock
    private RateLimitAdjustmentRepository adjustmentRepository;

    @Mock
    private KafkaEventPublisherService kafkaEventPublisherService;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyDetectionService(
                new AnomalyDetectionProperties(
                        60,
                        3,
                        new BigDecimal("1.0000"),
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.5000"),
                        60000,
                        new BigDecimal("0.1000"),
                        new BigDecimal("10.0000")
                ),
                routeMetricRepository,
                snapshotRepository,
                anomalyRecordRepository,
                alertRepository,
                assignmentRepository,
                policyRepository,
                adjustmentRepository,
                kafkaEventPublisherService,
                new AnomalyDetectionMapper()
        );
    }

    @Test
    void runDetectionCycleCreatesSnapshotAnomalyAndAlertForMetricSpike() {
        GatewayRoute route = route();
        when(routeMetricRepository.findByCalculatedAtGreaterThanEqualAndDeletedAtIsNullOrderByRouteIdAscCalculatedAtAsc(any()))
                .thenReturn(List.of(
                        metric(route, "0.100000", "0.000000"),
                        metric(route, "0.100000", "0.000000"),
                        metric(route, "0.900000", "0.000000")
                ));
        when(snapshotRepository.save(any(AnomalyStatSnapshot.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(anomalyRecordRepository.save(any(AnomalyRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(assignmentRepository.findActiveAdaptiveRouteAssignments(any())).thenReturn(List.of());

        var response = service.runDetectionCycle();

        assertThat(response.routeMetricsEvaluated()).isEqualTo(3);
        assertThat(response.statisticSnapshotsCreated()).isEqualTo(2);
        assertThat(response.anomaliesCreated()).isEqualTo(1);
        assertThat(response.alertsCreated()).isEqualTo(1);
        verify(alertRepository).save(any(Alert.class));
    }

    private static RouteMetric metric(GatewayRoute route, String blockedRate, String errorRate) {
        RouteMetric metric = new RouteMetric();
        metric.setRoute(route);
        metric.setCalculatedAt(Instant.now());
        metric.setWindowSeconds(300);
        metric.setRequestsPerSecond(new BigDecimal("1.000000"));
        metric.setBlockedRate(new BigDecimal(blockedRate));
        metric.setErrorRate(new BigDecimal(errorRate));
        metric.setAvgLatencyMs(new BigDecimal("15.0000"));
        return metric;
    }

    private static GatewayRoute route() {
        GatewayRoute route = new GatewayRoute();
        ReflectionTestUtils.setField(route, "id", UUID.randomUUID());
        route.setRouteKey("orders");
        route.setName("Orders");
        return route;
    }

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
