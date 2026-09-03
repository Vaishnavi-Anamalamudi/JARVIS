package com.adaptivegateway.analytics.service;

import com.adaptivegateway.analytics.dto.AnalyticsRollupResponse;
import com.adaptivegateway.analytics.dto.AnalyticsRunResponse;
import com.adaptivegateway.analytics.dto.AnalyticsSummaryResponse;
import com.adaptivegateway.analytics.dto.ClientMetricResponse;
import com.adaptivegateway.analytics.dto.ClientMetricsRunResponse;
import com.adaptivegateway.analytics.entity.AnalyticsRollup;
import com.adaptivegateway.analytics.entity.ClientMetric;
import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import com.adaptivegateway.analytics.mapper.AnalyticsMapper;
import com.adaptivegateway.analytics.repository.AnalyticsRollupRepository;
import com.adaptivegateway.analytics.repository.ClientMetricRepository;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.consumer.repository.ApiConsumerRepository;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.ratelimit.repository.RequestLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final List<String> ROLLUP_SORTS = List.of("bucketStart", "bucketEnd", "totalRequests", "blockedRequests", "errorRequests", "avgResponseTimeMs", "p95ResponseTimeMs");
    private static final List<String> CLIENT_METRIC_SORTS = List.of("calculatedAt", "requestsPerSecond", "blockedRate", "errorRate", "avgLatencyMs");

    private final RequestLogRepository requestLogRepository;
    private final AnalyticsRollupRepository rollupRepository;
    private final ClientMetricRepository clientMetricRepository;
    private final GatewayRouteRepository routeRepository;
    private final ApiConsumerRepository consumerRepository;
    private final AnalyticsMapper mapper;
    private final LiveEventService liveEventService;

    public AnalyticsService(
            RequestLogRepository requestLogRepository,
            AnalyticsRollupRepository rollupRepository,
            ClientMetricRepository clientMetricRepository,
            GatewayRouteRepository routeRepository,
            ApiConsumerRepository consumerRepository,
            AnalyticsMapper mapper,
            LiveEventService liveEventService
    ) {
        this.requestLogRepository = requestLogRepository;
        this.rollupRepository = rollupRepository;
        this.clientMetricRepository = clientMetricRepository;
        this.routeRepository = routeRepository;
        this.consumerRepository = consumerRepository;
        this.mapper = mapper;
        this.liveEventService = liveEventService;
    }

    @Transactional
    public AnalyticsRunResponse runRollups(AnalyticsGranularity granularity, int lookbackBuckets) {
        Instant startedAt = Instant.now();
        Duration bucketSize = bucketSize(granularity);
        Instant exclusiveEnd = truncate(startedAt, granularity);
        int written = 0;
        for (int index = lookbackBuckets; index >= 1; index--) {
            Instant bucketStart = exclusiveEnd.minus(bucketSize.multipliedBy(index));
            Instant bucketEnd = bucketStart.plus(bucketSize);
            written += writeRollupsForBucket(granularity, bucketStart, bucketEnd);
        }
        AnalyticsRunResponse response = new AnalyticsRunResponse(granularity, lookbackBuckets, written, startedAt, Instant.now());
        liveEventService.emit(
                "gateway.analytics.rollups_completed",
                "analytics_rollup",
                null,
                null,
                "Analytics rollups completed",
                Map.of(
                        "granularity", granularity.name(),
                        "bucketsEvaluated", lookbackBuckets,
                        "rollupsWritten", written
                )
        );
        return response;
    }

    @Transactional
    public ClientMetricsRunResponse runClientMetrics(int windowSeconds) {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minusSeconds(windowSeconds);
        int written = 0;
        for (RequestLogRepository.ClientTrafficAggregate aggregate : requestLogRepository.aggregateClientTraffic(windowStart, windowEnd)) {
            ClientMetric metric = new ClientMetric();
            metric.setConsumer(consumerRepository.getReferenceById(aggregate.getConsumerId()));
            metric.setCalculatedAt(windowEnd);
            metric.setWindowSeconds(windowSeconds);
            metric.setRequestsPerSecond(ratio(aggregate.getTotalRequests(), windowSeconds));
            metric.setBlockedRate(rate(aggregate.getBlockedRequests(), aggregate.getTotalRequests()));
            metric.setErrorRate(rate(aggregate.getErrorRequests(), aggregate.getTotalRequests()));
            metric.setAvgLatencyMs(scale(aggregate.getAvgLatencyMs(), 4));
            clientMetricRepository.save(metric);
            written++;
        }
        ClientMetricsRunResponse response = new ClientMetricsRunResponse(windowStart, windowEnd, windowSeconds, written, Instant.now());
        liveEventService.emit(
                "gateway.analytics.client_metrics_completed",
                "client_metric",
                null,
                null,
                "Client metrics completed",
                Map.of(
                        "windowSeconds", windowSeconds,
                        "metricsWritten", written
                )
        );
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalyticsRollupResponse> listRollups(
            AnalyticsGranularity granularity,
            UUID consumerId,
            UUID routeId,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, ROLLUP_SORTS, "bucketStart");
        return PageResponse.from(rollupRepository.search(granularity, consumerId, routeId, pageRequest).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientMetricResponse> listClientMetrics(
            UUID consumerId,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, CLIENT_METRIC_SORTS, "calculatedAt");
        return PageResponse.from(clientMetricRepository.search(consumerId, pageRequest).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(int windowSeconds) {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minusSeconds(windowSeconds);
        RequestLogRepository.AnalyticsSummaryAggregate aggregate = requestLogRepository.summarizeTraffic(windowStart, windowEnd);
        long totalRequests = aggregate.getTotalRequests();
        long blockedRequests = aggregate.getBlockedRequests();
        long errorRequests = aggregate.getErrorRequests();
        return new AnalyticsSummaryResponse(
                windowStart,
                windowEnd,
                windowSeconds,
                totalRequests,
                aggregate.getAllowedRequests(),
                blockedRequests,
                errorRequests,
                rate(blockedRequests, totalRequests),
                rate(errorRequests, totalRequests),
                scale(aggregate.getAvgResponseTimeMs(), 4),
                scale(aggregate.getP95ResponseTimeMs(), 4),
                aggregate.getUniqueSourceIps()
        );
    }

    private int writeRollupsForBucket(AnalyticsGranularity granularity, Instant bucketStart, Instant bucketEnd) {
        int written = 0;
        for (RequestLogRepository.AnalyticsTrafficAggregate aggregate : requestLogRepository.aggregateAnalyticsRollups(bucketStart, bucketEnd)) {
            AnalyticsRollup rollup = rollupRepository.findExisting(bucketStart, bucketEnd, granularity, aggregate.getConsumerId(), aggregate.getRouteId())
                    .orElseGet(AnalyticsRollup::new);
            rollup.setBucketStart(bucketStart);
            rollup.setBucketEnd(bucketEnd);
            rollup.setGranularity(granularity);
            rollup.setConsumer(aggregate.getConsumerId() == null ? null : consumerRepository.getReferenceById(aggregate.getConsumerId()));
            rollup.setRoute(aggregate.getRouteId() == null ? null : routeRepository.getReferenceById(aggregate.getRouteId()));
            rollup.setTotalRequests(aggregate.getTotalRequests());
            rollup.setAllowedRequests(aggregate.getAllowedRequests());
            rollup.setBlockedRequests(aggregate.getBlockedRequests());
            rollup.setErrorRequests(aggregate.getErrorRequests());
            rollup.setAvgResponseTimeMs(scale(aggregate.getAvgResponseTimeMs(), 4));
            rollup.setP95ResponseTimeMs(scale(aggregate.getP95ResponseTimeMs(), 4));
            rollup.setUniqueSourceIps(aggregate.getUniqueSourceIps());
            rollupRepository.save(rollup);
            written++;
        }
        return written;
    }

    private Duration bucketSize(AnalyticsGranularity granularity) {
        return switch (granularity) {
            case MINUTE -> Duration.ofMinutes(1);
            case HOUR -> Duration.ofHours(1);
            case DAY -> Duration.ofDays(1);
        };
    }

    private Instant truncate(Instant instant, AnalyticsGranularity granularity) {
        return switch (granularity) {
            case MINUTE -> instant.truncatedTo(ChronoUnit.MINUTES);
            case HOUR -> instant.truncatedTo(ChronoUnit.HOURS);
            case DAY -> instant.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant();
        };
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
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
