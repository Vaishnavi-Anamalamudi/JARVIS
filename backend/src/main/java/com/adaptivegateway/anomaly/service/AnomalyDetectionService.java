package com.adaptivegateway.anomaly.service;

import com.adaptivegateway.adaptive.entity.RateLimitAdjustment;
import com.adaptivegateway.adaptive.entity.RouteMetric;
import com.adaptivegateway.adaptive.repository.RateLimitAdjustmentRepository;
import com.adaptivegateway.adaptive.repository.RouteMetricRepository;
import com.adaptivegateway.alerts.entity.Alert;
import com.adaptivegateway.alerts.enums.AlertSeverity;
import com.adaptivegateway.alerts.enums.AlertStatus;
import com.adaptivegateway.alerts.repository.AlertRepository;
import com.adaptivegateway.anomaly.config.AnomalyDetectionProperties;
import com.adaptivegateway.anomaly.dto.AnomalyDetectionRunResponse;
import com.adaptivegateway.anomaly.dto.AnomalyRecordResponse;
import com.adaptivegateway.anomaly.dto.AnomalyStatSnapshotResponse;
import com.adaptivegateway.anomaly.entity.AnomalyRecord;
import com.adaptivegateway.anomaly.entity.AnomalyStatSnapshot;
import com.adaptivegateway.anomaly.enums.AnomalySeverity;
import com.adaptivegateway.anomaly.enums.AnomalyStatus;
import com.adaptivegateway.anomaly.mapper.AnomalyDetectionMapper;
import com.adaptivegateway.anomaly.repository.AnomalyRecordRepository;
import com.adaptivegateway.anomaly.repository.AnomalyStatSnapshotRepository;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.kafka.dto.KafkaEventRequest;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);
    private static final String BLOCKED_RATE = "blocked_rate";
    private static final String ERROR_RATE = "error_rate";

    private final AnomalyDetectionProperties properties;
    private final RouteMetricRepository routeMetricRepository;
    private final AnomalyStatSnapshotRepository snapshotRepository;
    private final AnomalyRecordRepository anomalyRecordRepository;
    private final AlertRepository alertRepository;
    private final RateLimitAssignmentRepository assignmentRepository;
    private final RateLimitPolicyRepository policyRepository;
    private final RateLimitAdjustmentRepository adjustmentRepository;
    private final KafkaEventPublisherService kafkaEventPublisherService;
    private final AnomalyDetectionMapper mapper;
    private final LiveEventService liveEventService;

    public AnomalyDetectionService(
            AnomalyDetectionProperties properties,
            RouteMetricRepository routeMetricRepository,
            AnomalyStatSnapshotRepository snapshotRepository,
            AnomalyRecordRepository anomalyRecordRepository,
            AlertRepository alertRepository,
            RateLimitAssignmentRepository assignmentRepository,
            RateLimitPolicyRepository policyRepository,
            RateLimitAdjustmentRepository adjustmentRepository,
            KafkaEventPublisherService kafkaEventPublisherService,
            AnomalyDetectionMapper mapper,
            LiveEventService liveEventService
    ) {
        this.properties = properties;
        this.routeMetricRepository = routeMetricRepository;
        this.snapshotRepository = snapshotRepository;
        this.anomalyRecordRepository = anomalyRecordRepository;
        this.alertRepository = alertRepository;
        this.assignmentRepository = assignmentRepository;
        this.policyRepository = policyRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.kafkaEventPublisherService = kafkaEventPublisherService;
        this.mapper = mapper;
        this.liveEventService = liveEventService;
    }

    @Transactional
    public AnomalyDetectionRunResponse runDetectionCycle() {
        Instant evaluatedSince = Instant.now().minus(properties.lookbackMinutes(), ChronoUnit.MINUTES);
        List<RouteMetric> metrics = routeMetricRepository
                .findByCalculatedAtGreaterThanEqualAndDeletedAtIsNullOrderByRouteIdAscCalculatedAtAsc(evaluatedSince);
        Map<UUID, List<RouteMetric>> byRoute = metrics.stream()
                .collect(Collectors.groupingBy(metric -> metric.getRoute().getId(), LinkedHashMap::new, Collectors.toList()));

        int snapshots = 0;
        int anomalies = 0;
        int alerts = 0;
        int adjustments = 0;
        for (List<RouteMetric> routeMetrics : byRoute.values()) {
            if (routeMetrics.size() < properties.minimumSamples()) {
                continue;
            }
            DetectionResult blockedRate = evaluate(routeMetrics, BLOCKED_RATE, RouteMetric::getBlockedRate);
            DetectionResult errorRate = evaluate(routeMetrics, ERROR_RATE, RouteMetric::getErrorRate);
            for (DetectionResult result : List.of(blockedRate, errorRate)) {
                AnomalyStatSnapshot snapshot = snapshotRepository.save(toSnapshot(routeMetrics.getLast(), result, routeMetrics.size()));
                snapshots++;
                if (result.anomalous()) {
                    AnomalyRecord anomaly = anomalyRecordRepository.save(toAnomaly(snapshot, result));
                    anomalies++;
                    Alert alert = alertRepository.save(toAlert(anomaly));
                    alerts++;
                    adjustments += tightenAdaptivePolicies(anomaly);
                    publishAnomaly(anomaly);
                    emitAnomaly(anomaly, alert);
                }
            }
        }

        AnomalyDetectionRunResponse response = new AnomalyDetectionRunResponse(evaluatedSince, metrics.size(), snapshots, anomalies, alerts, adjustments);
        liveEventService.emit(
                "gateway.anomaly.run_completed",
                "anomaly_detection",
                null,
                null,
                "Anomaly detection cycle completed",
                Map.of(
                        "routeMetricsEvaluated", metrics.size(),
                        "statisticSnapshotsCreated", snapshots,
                        "anomaliesCreated", anomalies,
                        "alertsCreated", alerts,
                        "strictnessAdjustmentsWritten", adjustments
                )
        );
        return response;
    }

    @Scheduled(fixedDelayString = "${adaptive-gateway.anomaly-detection.detection-interval-ms}")
    public void runScheduledDetectionCycle() {
        runDetectionCycle();
    }

    @Transactional(readOnly = true)
    public PageResponse<AnomalyRecordResponse> listAnomalies(
            AnomalyStatus status,
            UUID routeId,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, List.of("createdAt", "updatedAt", "severity", "status"), "createdAt");
        var anomalies = status != null && routeId != null
                ? anomalyRecordRepository.findByStatusAndRouteIdAndDeletedAtIsNull(status, routeId, pageRequest)
                : status != null
                ? anomalyRecordRepository.findByStatusAndDeletedAtIsNull(status, pageRequest)
                : routeId != null
                ? anomalyRecordRepository.findByRouteIdAndDeletedAtIsNull(routeId, pageRequest)
                : anomalyRecordRepository.findByDeletedAtIsNull(pageRequest);
        return PageResponse.from(anomalies.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<AnomalyStatSnapshotResponse> listSnapshots(
            UUID routeId,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, List.of("calculatedAt", "metricName", "rollingZScore"), "calculatedAt");
        var snapshots = routeId == null
                ? snapshotRepository.findByDeletedAtIsNull(pageRequest)
                : snapshotRepository.findByRouteIdAndDeletedAtIsNull(routeId, pageRequest);
        return PageResponse.from(snapshots.map(mapper::toResponse));
    }

    private DetectionResult evaluate(List<RouteMetric> metrics, String metricName, Function<RouteMetric, BigDecimal> extractor) {
        List<BigDecimal> values = metrics.stream().map(extractor).toList();
        BigDecimal observed = values.getLast();
        BigDecimal mean = mean(values);
        BigDecimal variance = variance(values, mean);
        BigDecimal stddev = sqrt(variance);
        BigDecimal zScore = stddev.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : observed.subtract(mean).divide(stddev, 6, RoundingMode.HALF_UP);
        BigDecimal ema = ema(values);
        BigDecimal adaptiveThreshold = mean.add(stddev.multiply(properties.thresholdStddevMultiplier())).setScale(6, RoundingMode.HALF_UP);
        boolean anomalous = zScore.compareTo(properties.zScoreThreshold()) >= 0
                && observed.compareTo(adaptiveThreshold) > 0;
        return new DetectionResult(metricName, observed, mean, variance, stddev, zScore, ema, adaptiveThreshold, anomalous);
    }

    private AnomalyStatSnapshot toSnapshot(RouteMetric latestMetric, DetectionResult result, int sampleCount) {
        AnomalyStatSnapshot snapshot = new AnomalyStatSnapshot();
        snapshot.setRoute(latestMetric.getRoute());
        snapshot.setMetricName(result.metricName());
        snapshot.setWindowSeconds(latestMetric.getWindowSeconds());
        snapshot.setSampleCount(sampleCount);
        snapshot.setRollingMean(result.mean());
        snapshot.setRollingVariance(result.variance());
        snapshot.setRollingStddev(result.stddev());
        snapshot.setRollingZScore(result.zScore());
        snapshot.setEmaValue(result.ema());
        snapshot.setAdaptiveThreshold(result.threshold());
        snapshot.setCalculatedAt(Instant.now());
        return snapshot;
    }

    private AnomalyRecord toAnomaly(AnomalyStatSnapshot snapshot, DetectionResult result) {
        AnomalyRecord record = new AnomalyRecord();
        record.setStatsSnapshot(snapshot);
        record.setRoute(snapshot.getRoute());
        record.setSeverity(severity(result.zScore()));
        record.setMetricName(result.metricName());
        record.setObservedValue(result.observed());
        record.setThresholdValue(result.threshold());
        record.setZScore(result.zScore());
        record.setStatus(AnomalyStatus.OPEN);
        record.setDescription("Detected route metric spike for %s: observed=%s threshold=%s zScore=%s"
                .formatted(result.metricName(), result.observed(), result.threshold(), result.zScore()));
        return record;
    }

    private Alert toAlert(AnomalyRecord anomaly) {
        Alert alert = new Alert();
        alert.setAnomalyRecord(anomaly);
        alert.setRoute(anomaly.getRoute());
        alert.setAlertType("ANOMALY_DETECTED");
        alert.setSeverity(AlertSeverity.valueOf(anomaly.getSeverity().name()));
        alert.setTitle("Anomaly detected on route " + anomaly.getRoute().getRouteKey());
        alert.setMessage(anomaly.getDescription());
        alert.setStatus(AlertStatus.OPEN);
        return alert;
    }

    private int tightenAdaptivePolicies(AnomalyRecord anomaly) {
        List<RateLimitAssignment> assignments = assignmentRepository.findActiveAdaptiveRouteAssignments(Instant.now())
                .stream()
                .filter(assignment -> assignment.getRoute().getId().equals(anomaly.getRoute().getId()))
                .toList();
        int count = 0;
        for (RateLimitAssignment assignment : assignments) {
            RateLimitPolicy policy = assignment.getPolicy();
            BigDecimal previous = policy.getStrictnessFactor();
            BigDecimal next = previous.add(properties.strictnessIncreaseStep())
                    .min(properties.maxStrictnessFactor())
                    .setScale(4, RoundingMode.HALF_UP);
            if (next.compareTo(previous) == 0) {
                continue;
            }
            policy.setStrictnessFactor(next);
            policyRepository.save(policy);
            RateLimitAdjustment adjustment = new RateLimitAdjustment();
            adjustment.setPolicy(policy);
            adjustment.setRoute(assignment.getRoute());
            adjustment.setAnomalyRecordId(anomaly.getId());
            adjustment.setPreviousStrictnessFactor(previous);
            adjustment.setNewStrictnessFactor(next);
            adjustment.setReason("Anomaly detection increased strictness for " + anomaly.getMetricName());
            adjustmentRepository.save(adjustment);
            count++;
        }
        return count;
    }

    private void publishAnomaly(AnomalyRecord anomaly) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            GatewayRoute route = anomaly.getRoute();
            payload.put("anomalyId", anomaly.getId().toString());
            payload.put("routeId", route.getId().toString());
            payload.put("routeKey", route.getRouteKey());
            payload.put("severity", anomaly.getSeverity().name());
            payload.put("metricName", anomaly.getMetricName());
            payload.put("observedValue", anomaly.getObservedValue());
            payload.put("thresholdValue", anomaly.getThresholdValue());
            payload.put("zScore", anomaly.getZScore());
            kafkaEventPublisherService.recordAndPublish(new KafkaEventRequest(
                    null,
                    "gateway.anomaly.detected",
                    "anomaly_record",
                    anomaly.getId(),
                    null,
                    payload
            ));
        } catch (RuntimeException exception) {
            log.warn("Anomaly Kafka event publication failed: anomalyId={}", anomaly.getId(), exception);
        }
    }

    private void emitAnomaly(AnomalyRecord anomaly, Alert alert) {
        GatewayRoute route = anomaly.getRoute();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("anomalyId", anomaly.getId().toString());
        payload.put("alertId", alert.getId() == null ? null : alert.getId().toString());
        payload.put("routeId", route.getId().toString());
        payload.put("routeKey", route.getRouteKey());
        payload.put("severity", anomaly.getSeverity().name());
        payload.put("metricName", anomaly.getMetricName());
        payload.put("observedValue", anomaly.getObservedValue());
        payload.put("thresholdValue", anomaly.getThresholdValue());
        payload.put("zScore", anomaly.getZScore());
        liveEventService.emit(
                "gateway.anomaly.detected",
                "anomaly_record",
                anomaly.getId(),
                null,
                "Anomaly detected",
                payload
        );
    }

    private AnomalySeverity severity(BigDecimal zScore) {
        if (zScore.compareTo(BigDecimal.valueOf(6)) >= 0) {
            return AnomalySeverity.CRITICAL;
        }
        if (zScore.compareTo(BigDecimal.valueOf(4)) >= 0) {
            return AnomalySeverity.HIGH;
        }
        if (zScore.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return AnomalySeverity.MEDIUM;
        }
        return AnomalySeverity.LOW;
    }

    private BigDecimal mean(List<BigDecimal> values) {
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal variance(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal total = values.stream()
                .map(value -> value.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal sqrt(BigDecimal value) {
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue())).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal ema(List<BigDecimal> values) {
        BigDecimal alpha = properties.emaAlpha();
        BigDecimal ema = values.getFirst();
        for (int index = 1; index < values.size(); index++) {
            ema = values.get(index).multiply(alpha).add(ema.multiply(BigDecimal.ONE.subtract(alpha)));
        }
        return ema.setScale(6, RoundingMode.HALF_UP);
    }

    private PageRequest pageRequest(int page, int size, String sort, String direction, List<String> allowedSorts, String defaultSort) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }

    private record DetectionResult(
            String metricName,
            BigDecimal observed,
            BigDecimal mean,
            BigDecimal variance,
            BigDecimal stddev,
            BigDecimal zScore,
            BigDecimal ema,
            BigDecimal threshold,
            boolean anomalous
    ) {
    }
}
