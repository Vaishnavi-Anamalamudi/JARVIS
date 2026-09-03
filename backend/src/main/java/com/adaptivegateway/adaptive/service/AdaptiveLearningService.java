package com.adaptivegateway.adaptive.service;

import com.adaptivegateway.adaptive.config.AdaptiveLearningProperties;
import com.adaptivegateway.adaptive.dto.AdaptiveLearningRunResponse;
import com.adaptivegateway.adaptive.entity.RateLimitAdjustment;
import com.adaptivegateway.adaptive.entity.RouteMetric;
import com.adaptivegateway.adaptive.entity.TrafficHeatmapBucket;
import com.adaptivegateway.adaptive.repository.RateLimitAdjustmentRepository;
import com.adaptivegateway.adaptive.repository.RouteMetricRepository;
import com.adaptivegateway.adaptive.repository.TrafficHeatmapBucketRepository;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.kafka.dto.KafkaEventRequest;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import com.adaptivegateway.ratelimit.repository.RequestLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdaptiveLearningService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveLearningService.class);

    private final AdaptiveLearningProperties properties;
    private final RequestLogRepository requestLogRepository;
    private final GatewayRouteRepository routeRepository;
    private final RouteMetricRepository routeMetricRepository;
    private final TrafficHeatmapBucketRepository heatmapRepository;
    private final RateLimitAssignmentRepository assignmentRepository;
    private final RateLimitPolicyRepository policyRepository;
    private final RateLimitAdjustmentRepository adjustmentRepository;
    private final KafkaEventPublisherService kafkaEventPublisherService;
    private final LiveEventService liveEventService;

    public AdaptiveLearningService(
            AdaptiveLearningProperties properties,
            RequestLogRepository requestLogRepository,
            GatewayRouteRepository routeRepository,
            RouteMetricRepository routeMetricRepository,
            TrafficHeatmapBucketRepository heatmapRepository,
            RateLimitAssignmentRepository assignmentRepository,
            RateLimitPolicyRepository policyRepository,
            RateLimitAdjustmentRepository adjustmentRepository,
            KafkaEventPublisherService kafkaEventPublisherService,
            LiveEventService liveEventService
    ) {
        this.properties = properties;
        this.requestLogRepository = requestLogRepository;
        this.routeRepository = routeRepository;
        this.routeMetricRepository = routeMetricRepository;
        this.heatmapRepository = heatmapRepository;
        this.assignmentRepository = assignmentRepository;
        this.policyRepository = policyRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.kafkaEventPublisherService = kafkaEventPublisherService;
        this.liveEventService = liveEventService;
    }

    @Transactional
    public AdaptiveLearningRunResponse runLearningCycle() {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minusSeconds(properties.metricWindowSeconds());
        List<RequestLogRepository.RouteTrafficAggregate> aggregates = requestLogRepository.aggregateRouteTraffic(windowStart, windowEnd);
        int routeMetrics = writeRouteMetrics(aggregates, windowEnd);
        int heatmapBuckets = writeHeatmapBuckets(windowEnd.truncatedTo(ChronoUnit.HOURS), aggregates);
        int adjustments = writeStrictnessAdjustments(aggregates, windowEnd);
        AdaptiveLearningRunResponse response = new AdaptiveLearningRunResponse(
                windowStart,
                windowEnd,
                properties.metricWindowSeconds(),
                routeMetrics,
                heatmapBuckets,
                adjustments,
                Instant.now()
        );
        liveEventService.emit(
                "gateway.adaptive.run_completed",
                "adaptive_learning",
                null,
                null,
                "Adaptive learning cycle completed",
                Map.of(
                        "routeMetricsWritten", routeMetrics,
                        "heatmapBucketsWritten", heatmapBuckets,
                        "strictnessAdjustmentsWritten", adjustments,
                        "metricWindowSeconds", properties.metricWindowSeconds()
                )
        );
        return response;
    }

    @Scheduled(fixedDelayString = "${adaptive-gateway.adaptive-learning.aggregation-interval-ms}")
    public void runScheduledLearningCycle() {
        runLearningCycle();
    }

    private int writeRouteMetrics(List<RequestLogRepository.RouteTrafficAggregate> aggregates, Instant calculatedAt) {
        int written = 0;
        for (RequestLogRepository.RouteTrafficAggregate aggregate : aggregates) {
            RouteMetric metric = new RouteMetric();
            metric.setRoute(routeRepository.getReferenceById(aggregate.getRouteId()));
            metric.setCalculatedAt(calculatedAt);
            metric.setWindowSeconds(properties.metricWindowSeconds());
            metric.setRequestsPerSecond(ratio(aggregate.getTotalRequests(), properties.metricWindowSeconds()));
            metric.setBlockedRate(rate(aggregate.getBlockedRequests(), aggregate.getTotalRequests()));
            metric.setErrorRate(rate(aggregate.getErrorRequests(), aggregate.getTotalRequests()));
            metric.setAvgLatencyMs(scale(aggregate.getAvgLatencyMs(), 4));
            routeMetricRepository.save(metric);
            written++;
        }
        return written;
    }

    private int writeHeatmapBuckets(Instant bucketStart, List<RequestLogRepository.RouteTrafficAggregate> aggregates) {
        int written = 0;
        short dayOfWeek = (short) (bucketStart.atZone(ZoneOffset.UTC).getDayOfWeek().getValue() % 7);
        short hourOfDay = (short) bucketStart.atZone(ZoneOffset.UTC).getHour();
        for (RequestLogRepository.RouteTrafficAggregate aggregate : aggregates) {
            TrafficHeatmapBucket bucket = heatmapRepository
                    .findByBucketStartAndRouteIdAndConsumerIdIsNullAndDeletedAtIsNull(bucketStart, aggregate.getRouteId())
                    .orElseGet(TrafficHeatmapBucket::new);
            bucket.setBucketStart(bucketStart);
            bucket.setDayOfWeek(dayOfWeek);
            bucket.setHourOfDay(hourOfDay);
            bucket.setRoute(routeRepository.getReferenceById(aggregate.getRouteId()));
            bucket.setRequestCount(aggregate.getTotalRequests());
            bucket.setBlockedCount(aggregate.getBlockedRequests());
            bucket.setAvgLatencyMs(scale(aggregate.getAvgLatencyMs(), 4));
            heatmapRepository.save(bucket);
            written++;
        }
        return written;
    }

    private int writeStrictnessAdjustments(List<RequestLogRepository.RouteTrafficAggregate> aggregates, Instant now) {
        Map<UUID, RequestLogRepository.RouteTrafficAggregate> byRoute = new LinkedHashMap<>();
        aggregates.forEach(aggregate -> byRoute.put(aggregate.getRouteId(), aggregate));

        int written = 0;
        for (RateLimitAssignment assignment : assignmentRepository.findActiveAdaptiveRouteAssignments(now)) {
            RequestLogRepository.RouteTrafficAggregate aggregate = byRoute.get(assignment.getRoute().getId());
            if (aggregate == null || aggregate.getTotalRequests() < properties.minimumRequestsForAdjustment()) {
                continue;
            }
            BigDecimal nextStrictness = nextStrictness(assignment.getPolicy(), aggregate);
            if (nextStrictness.compareTo(assignment.getPolicy().getStrictnessFactor()) == 0) {
                continue;
            }
            saveAdjustment(assignment, nextStrictness, reason(aggregate));
            written++;
        }
        return written;
    }

    private BigDecimal nextStrictness(RateLimitPolicy policy, RequestLogRepository.RouteTrafficAggregate aggregate) {
        BigDecimal current = policy.getStrictnessFactor();
        BigDecimal blockedRate = rate(aggregate.getBlockedRequests(), aggregate.getTotalRequests());
        BigDecimal errorRate = rate(aggregate.getErrorRequests(), aggregate.getTotalRequests());
        if (errorRate.compareTo(properties.errorRateTightenThreshold()) >= 0) {
            return current.add(properties.strictnessAdjustmentStep()).min(properties.maxStrictnessFactor()).setScale(4, RoundingMode.HALF_UP);
        }
        if (blockedRate.compareTo(properties.blockedRateRelaxThreshold()) >= 0
                && errorRate.compareTo(properties.maxErrorRateForRelaxation()) <= 0) {
            return current.subtract(properties.strictnessAdjustmentStep()).max(properties.minStrictnessFactor()).setScale(4, RoundingMode.HALF_UP);
        }
        return current.setScale(4, RoundingMode.HALF_UP);
    }

    private void saveAdjustment(RateLimitAssignment assignment, BigDecimal nextStrictness, String reason) {
        RateLimitPolicy policy = assignment.getPolicy();
        BigDecimal previous = policy.getStrictnessFactor();
        policy.setStrictnessFactor(nextStrictness);
        policyRepository.save(policy);

        RateLimitAdjustment adjustment = new RateLimitAdjustment();
        adjustment.setPolicy(policy);
        adjustment.setRoute(assignment.getRoute());
        adjustment.setPreviousStrictnessFactor(previous);
        adjustment.setNewStrictnessFactor(nextStrictness);
        adjustment.setReason(reason);
        RateLimitAdjustment saved = adjustmentRepository.save(adjustment);
        publishAdjustment(saved, assignment.getRoute());
        liveEventService.emit(
                "gateway.adaptive.strictness_adjusted",
                "rate_limit_adjustment",
                saved.getId(),
                null,
                "Adaptive strictness adjusted",
                Map.of(
                        "policyId", policy.getId().toString(),
                        "routeId", assignment.getRoute().getId().toString(),
                        "routeKey", assignment.getRoute().getRouteKey(),
                        "previousStrictnessFactor", previous,
                        "newStrictnessFactor", nextStrictness,
                        "reason", reason
                )
        );
    }

    private void publishAdjustment(RateLimitAdjustment adjustment, GatewayRoute route) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("policyId", adjustment.getPolicy().getId().toString());
            payload.put("routeId", route.getId().toString());
            payload.put("routeKey", route.getRouteKey());
            payload.put("previousStrictnessFactor", adjustment.getPreviousStrictnessFactor());
            payload.put("newStrictnessFactor", adjustment.getNewStrictnessFactor());
            payload.put("reason", adjustment.getReason());
            kafkaEventPublisherService.recordAndPublish(new KafkaEventRequest(
                    null,
                    "gateway.adaptive.strictness_adjusted",
                    "rate_limit_policy",
                    adjustment.getPolicy().getId(),
                    null,
                    payload
            ));
        } catch (RuntimeException exception) {
            log.warn("Adaptive adjustment Kafka event publication failed: routeKey={}", route.getRouteKey(), exception);
        }
    }

    private String reason(RequestLogRepository.RouteTrafficAggregate aggregate) {
        return "Adaptive learning window observed totalRequests=%d, blockedRate=%s, errorRate=%s"
                .formatted(
                        aggregate.getTotalRequests(),
                        rate(aggregate.getBlockedRequests(), aggregate.getTotalRequests()),
                        rate(aggregate.getErrorRequests(), aggregate.getTotalRequests())
                );
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
}
