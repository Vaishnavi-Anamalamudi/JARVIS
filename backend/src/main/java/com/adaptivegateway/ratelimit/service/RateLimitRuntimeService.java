package com.adaptivegateway.ratelimit.service;

import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.gateway.entity.GatewayRoute;
import com.adaptivegateway.gateway.repository.GatewayRouteRepository;
import com.adaptivegateway.kafka.dto.KafkaEventRequest;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import com.adaptivegateway.ratelimit.dto.RateLimitEvaluation;
import com.adaptivegateway.ratelimit.dto.RateLimitRuntimeDecision;
import com.adaptivegateway.ratelimit.dto.RedisRateLimitResult;
import com.adaptivegateway.ratelimit.entity.RateLimitAssignment;
import com.adaptivegateway.ratelimit.entity.RateLimitDecision;
import com.adaptivegateway.ratelimit.entity.RateLimitPolicy;
import com.adaptivegateway.ratelimit.entity.RequestLog;
import com.adaptivegateway.ratelimit.enums.GatewayOutcome;
import com.adaptivegateway.ratelimit.enums.RateLimitAlgorithm;
import com.adaptivegateway.ratelimit.enums.RateLimitDecisionValue;
import com.adaptivegateway.ratelimit.repository.RateLimitAssignmentRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitDecisionRepository;
import com.adaptivegateway.ratelimit.repository.RateLimitPolicyRepository;
import com.adaptivegateway.ratelimit.repository.RequestLogRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ServerWebExchange;

@Service
public class RateLimitRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitRuntimeService.class);

    private final GatewayRouteRepository routeRepository;
    private final RateLimitAssignmentRepository assignmentRepository;
    private final RateLimitPolicyRepository policyRepository;
    private final RequestLogRepository requestLogRepository;
    private final RateLimitDecisionRepository decisionRepository;
    private final RedisRateLimitCounterService counterService;
    private final KafkaEventPublisherService kafkaEventPublisherService;

    public RateLimitRuntimeService(
            GatewayRouteRepository routeRepository,
            RateLimitAssignmentRepository assignmentRepository,
            RateLimitPolicyRepository policyRepository,
            RequestLogRepository requestLogRepository,
            RateLimitDecisionRepository decisionRepository,
            RedisRateLimitCounterService counterService,
            KafkaEventPublisherService kafkaEventPublisherService
    ) {
        this.routeRepository = routeRepository;
        this.assignmentRepository = assignmentRepository;
        this.policyRepository = policyRepository;
        this.requestLogRepository = requestLogRepository;
        this.decisionRepository = decisionRepository;
        this.counterService = counterService;
        this.kafkaEventPublisherService = kafkaEventPublisherService;
    }

    @Transactional(readOnly = true)
    public Optional<RateLimitEvaluation> evaluate(String routeKey, ServerWebExchange exchange) {
        Optional<GatewayRoute> route = routeRepository.findByRouteKeyIgnoreCaseAndDeletedAtIsNull(routeKey);
        if (route.isEmpty()) {
            return Optional.empty();
        }
        var assignments = assignmentRepository.findActiveRouteAssignments(
                route.get().getId(),
                Instant.now(),
                PageRequest.of(0, 1)
        );
        if (assignments.isEmpty()) {
            return Optional.of(new RateLimitEvaluation(route.get(), null));
        }
        RateLimitAssignment assignment = assignments.getFirst();
        return Optional.of(new RateLimitEvaluation(route.get(), decide(assignment, identifier(route.get(), exchange))));
    }

    @Transactional
    public void recordCompletion(
            RateLimitEvaluation evaluation,
            ServerWebExchange exchange,
            Instant startedAt,
            int statusCode,
            String errorCode
    ) {
        RequestLog requestLog = buildRequestLog(evaluation.route(), exchange, startedAt, statusCode, errorCode);
        RequestLog savedLog = requestLogRepository.save(requestLog);
        evaluation.decisionOptional().ifPresent(decision -> saveDecision(savedLog, decision));
        evaluation.decisionOptional().ifPresent(decision -> publishDecisionEvent(savedLog, evaluation.route(), decision, exchange));
    }

    private RateLimitRuntimeDecision decide(RateLimitAssignment assignment, String identifier) {
        RateLimitPolicy policy = assignment.getPolicy();
        int effectiveLimit = effectiveLimit(policy.getMaxRequests(), policy);
        RedisRateLimitResult result = switch (policy.getAlgorithm()) {
            case SLIDING_WINDOW -> counterService.slidingWindow(
                    assignment.getRoute().getId(),
                    identifier,
                    effectiveLimit,
                    policy.getWindowSeconds()
            );
            case TOKEN_BUCKET -> counterService.tokenBucket(
                    assignment.getRoute().getId(),
                    identifier,
                    effectiveLimit(policy.getBucketCapacity(), policy),
                    policy.getRefillTokens(),
                    policy.getRefillPeriodSeconds()
            );
            case HYBRID -> hybrid(assignment, identifier, effectiveLimit);
        };
        return new RateLimitRuntimeDecision(
                policy.getId(),
                assignment.getId(),
                policy.getAlgorithm(),
                result.allowed() ? RateLimitDecisionValue.ALLOW : RateLimitDecisionValue.BLOCK,
                effectiveLimit,
                result.observedCount(),
                result.remainingTokens(),
                result.retryAfterSeconds(),
                policy.getStrictnessFactor()
        );
    }

    private RedisRateLimitResult hybrid(RateLimitAssignment assignment, String identifier, int effectiveLimit) {
        RateLimitPolicy policy = assignment.getPolicy();
        RedisRateLimitResult window = counterService.slidingWindow(
                assignment.getRoute().getId(),
                identifier,
                effectiveLimit,
                policy.getWindowSeconds()
        );
        RedisRateLimitResult bucket = counterService.tokenBucket(
                assignment.getRoute().getId(),
                identifier,
                effectiveLimit(policy.getBucketCapacity(), policy),
                policy.getRefillTokens(),
                policy.getRefillPeriodSeconds()
        );
        boolean allowed = window.allowed() && bucket.allowed();
        return new RedisRateLimitResult(
                allowed,
                window.observedCount(),
                bucket.remainingTokens(),
                Math.max(window.retryAfterSeconds(), bucket.retryAfterSeconds())
        );
    }

    private void saveDecision(RequestLog requestLog, RateLimitRuntimeDecision runtimeDecision) {
        RateLimitDecision decision = new RateLimitDecision();
        decision.setRequestLog(requestLog);
        decision.setPolicy(policyRepository.getReferenceById(runtimeDecision.policyId()));
        decision.setAssignment(assignmentRepository.getReferenceById(runtimeDecision.assignmentId()));
        decision.setAlgorithm(runtimeDecision.algorithm());
        decision.setDecision(runtimeDecision.decision());
        decision.setEffectiveLimit(runtimeDecision.effectiveLimit());
        decision.setObservedCount(runtimeDecision.observedCount());
        decision.setRemainingTokens(runtimeDecision.remainingTokens());
        decision.setRetryAfterSeconds(runtimeDecision.retryAfterSeconds());
        decision.setStrictnessFactor(runtimeDecision.strictnessFactor());
        decisionRepository.save(decision);
    }

    private RequestLog buildRequestLog(
            GatewayRoute route,
            ServerWebExchange exchange,
            Instant startedAt,
            int statusCode,
            String errorCode
    ) {
        ServerHttpRequest request = exchange.getRequest();
        Instant completedAt = Instant.now();
        RequestLog requestLog = new RequestLog();
        requestLog.setCorrelationId(correlationId(exchange));
        requestLog.setRoute(route);
        requestLog.setUpstreamService(route.getUpstreamService());
        requestLog.setRequestMethod(request.getMethod().name());
        requestLog.setRequestPath(request.getPath().value());
        requestLog.setRequestQueryHash(hashOrNull(request.getURI().getRawQuery()));
        requestLog.setRequestHeadersHash(hashOrNull(request.getHeaders().toString()));
        requestLog.setSourceIp(sourceIp(exchange));
        requestLog.setUserAgent(request.getHeaders().getFirst("User-Agent"));
        requestLog.setGatewayOutcome(outcome(statusCode));
        requestLog.setStatusCode(statusCode);
        requestLog.setResponseTimeMs(Math.toIntExact(Math.min(Duration.between(startedAt, completedAt).toMillis(), Integer.MAX_VALUE)));
        requestLog.setRequestBytes(Math.max(0, request.getHeaders().getContentLength()));
        requestLog.setResponseBytes(Math.max(0, exchange.getResponse().getHeaders().getContentLength()));
        requestLog.setErrorCode(errorCode);
        requestLog.setStartedAt(startedAt);
        requestLog.setCompletedAt(completedAt);
        return requestLog;
    }

    private void publishDecisionEvent(
            RequestLog requestLog,
            GatewayRoute route,
            RateLimitRuntimeDecision decision,
            ServerWebExchange exchange
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("routeId", route.getId().toString());
            payload.put("routeKey", route.getRouteKey());
            payload.put("policyId", decision.policyId().toString());
            payload.put("assignmentId", decision.assignmentId().toString());
            payload.put("decision", decision.decision().name());
            payload.put("algorithm", decision.algorithm().name());
            payload.put("effectiveLimit", decision.effectiveLimit());
            payload.put("observedCount", decision.observedCount());
            payload.put("remainingTokens", decision.remainingTokens());
            payload.put("retryAfterSeconds", decision.retryAfterSeconds());
            kafkaEventPublisherService.recordAndPublish(new KafkaEventRequest(
                    null,
                    "gateway.rate_limit.decision",
                    "request_log",
                    requestLog.getId(),
                    correlationId(exchange).toString(),
                    payload
            ));
        } catch (RuntimeException exception) {
            log.warn("Rate limit Kafka decision event publication failed: routeKey={}", route.getRouteKey(), exception);
        }
    }

    private int effectiveLimit(Integer configuredLimit, RateLimitPolicy policy) {
        double strictness = policy.getStrictnessFactor().doubleValue();
        return Math.max(1, (int) Math.floor(configuredLimit / strictness));
    }

    private String identifier(GatewayRoute route, ServerWebExchange exchange) {
        String sourceIp = sourceIp(exchange) == null ? "unknown" : sourceIp(exchange).getHostAddress();
        return route.getRouteKey() + ":" + sourceIp;
    }

    private GatewayOutcome outcome(int statusCode) {
        if (statusCode == 429 || statusCode == 401 || statusCode == 403) {
            return GatewayOutcome.BLOCKED;
        }
        return statusCode >= 500 ? GatewayOutcome.ERROR : GatewayOutcome.ALLOWED;
    }

    private UUID correlationId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        if (value == null) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            return UUID.nameUUIDFromBytes(value.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private InetAddress sourceIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null) {
            return null;
        }
        try {
            return InetAddress.getByName(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private String hashOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
