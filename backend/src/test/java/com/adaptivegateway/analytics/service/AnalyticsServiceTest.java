package com.adaptivegateway.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.analytics.entity.AnalyticsRollup;
import com.adaptivegateway.analytics.entity.ClientMetric;
import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import com.adaptivegateway.analytics.mapper.AnalyticsMapper;
import com.adaptivegateway.analytics.repository.AnalyticsRollupRepository;
import com.adaptivegateway.analytics.repository.ClientMetricRepository;
import com.adaptivegateway.consumer.entity.ApiConsumer;
import com.adaptivegateway.consumer.repository.ApiConsumerRepository;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.ratelimit.repository.RequestLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RequestLogRepository requestLogRepository;

    @Mock
    private AnalyticsRollupRepository rollupRepository;

    @Mock
    private ClientMetricRepository clientMetricRepository;

    @Mock
    private GatewayRouteRepository routeRepository;

    @Mock
    private ApiConsumerRepository consumerRepository;

    @Mock
    private LiveEventService liveEventService;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(
                requestLogRepository,
                rollupRepository,
                clientMetricRepository,
                routeRepository,
                consumerRepository,
                new AnalyticsMapper(),
                liveEventService
        );
    }

    @Test
    void runRollupsWritesAggregatesFromRequestLogs() {
        UUID routeId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();
        GatewayRoute route = new GatewayRoute();
        ReflectionTestUtils.setField(route, "id", routeId);
        route.setRouteKey("orders");
        ApiConsumer consumer = new ApiConsumer();
        ReflectionTestUtils.setField(consumer, "id", consumerId);
        consumer.setName("Orders client");

        when(requestLogRepository.aggregateAnalyticsRollups(any(), any()))
                .thenReturn(List.of(trafficAggregate(consumerId, routeId, 10, 7, 2, 1)));
        when(rollupRepository.findExisting(any(), any(), eq(AnalyticsGranularity.HOUR), eq(consumerId), eq(routeId)))
                .thenReturn(Optional.empty());
        when(routeRepository.getReferenceById(routeId)).thenReturn(route);
        when(consumerRepository.getReferenceById(consumerId)).thenReturn(consumer);

        var response = service.runRollups(AnalyticsGranularity.HOUR, 1);

        assertThat(response.rollupsWritten()).isEqualTo(1);
        ArgumentCaptor<AnalyticsRollup> rollupCaptor = ArgumentCaptor.forClass(AnalyticsRollup.class);
        verify(rollupRepository).save(rollupCaptor.capture());
        assertThat(rollupCaptor.getValue().getTotalRequests()).isEqualTo(10);
        assertThat(rollupCaptor.getValue().getAllowedRequests()).isEqualTo(7);
        assertThat(rollupCaptor.getValue().getBlockedRequests()).isEqualTo(2);
        assertThat(rollupCaptor.getValue().getErrorRequests()).isEqualTo(1);
        assertThat(rollupCaptor.getValue().getRoute().getRouteKey()).isEqualTo("orders");
        verify(liveEventService).emit(eq("gateway.analytics.rollups_completed"), eq("analytics_rollup"), any(), any(), any(), any());
    }

    @Test
    void runClientMetricsWritesPerConsumerRates() {
        UUID consumerId = UUID.randomUUID();
        ApiConsumer consumer = new ApiConsumer();
        ReflectionTestUtils.setField(consumer, "id", consumerId);
        consumer.setName("Orders client");

        when(requestLogRepository.aggregateClientTraffic(any(), any()))
                .thenReturn(List.of(clientAggregate(consumerId, 120, 12, 6)));
        when(consumerRepository.getReferenceById(consumerId)).thenReturn(consumer);

        var response = service.runClientMetrics(60);

        assertThat(response.metricsWritten()).isEqualTo(1);
        ArgumentCaptor<ClientMetric> metricCaptor = ArgumentCaptor.forClass(ClientMetric.class);
        verify(clientMetricRepository).save(metricCaptor.capture());
        assertThat(metricCaptor.getValue().getRequestsPerSecond()).isEqualByComparingTo("2.000000");
        assertThat(metricCaptor.getValue().getBlockedRate()).isEqualByComparingTo("0.100000");
        assertThat(metricCaptor.getValue().getErrorRate()).isEqualByComparingTo("0.050000");
        verify(liveEventService).emit(eq("gateway.analytics.client_metrics_completed"), eq("client_metric"), any(), any(), any(), any());
    }

    private RequestLogRepository.AnalyticsTrafficAggregate trafficAggregate(
            UUID consumerId,
            UUID routeId,
            long total,
            long allowed,
            long blocked,
            long errors
    ) {
        return new RequestLogRepository.AnalyticsTrafficAggregate() {
            @Override
            public UUID getConsumerId() {
                return consumerId;
            }

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
                return allowed;
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
            public BigDecimal getAvgResponseTimeMs() {
                return new BigDecimal("42.5000");
            }

            @Override
            public BigDecimal getP95ResponseTimeMs() {
                return new BigDecimal("95.0000");
            }

            @Override
            public int getUniqueSourceIps() {
                return 3;
            }
        };
    }

    private RequestLogRepository.ClientTrafficAggregate clientAggregate(UUID consumerId, long total, long blocked, long errors) {
        return new RequestLogRepository.ClientTrafficAggregate() {
            @Override
            public UUID getConsumerId() {
                return consumerId;
            }

            @Override
            public long getTotalRequests() {
                return total;
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
                return new BigDecimal("31.2500");
            }
        };
    }
}
