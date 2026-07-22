package com.adaptivegateway.ratelimit.web;

import com.adaptivegateway.common.api.ErrorResponse;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.ratelimit.dto.RateLimitEvaluation;
import com.adaptivegateway.ratelimit.dto.RateLimitRuntimeDecision;
import com.adaptivegateway.ratelimit.service.RateLimitRuntimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class RateLimitingGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingGatewayFilter.class);

    private final RateLimitRuntimeService rateLimitRuntimeService;
    private final ObjectMapper objectMapper;

    public RateLimitingGatewayFilter(RateLimitRuntimeService rateLimitRuntimeService, ObjectMapper objectMapper) {
        this.rateLimitRuntimeService = rateLimitRuntimeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route gatewayRoute = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (gatewayRoute == null) {
            return chain.filter(exchange);
        }

        Instant startedAt = Instant.now();
        return Mono.fromCallable(() -> rateLimitRuntimeService.evaluate(gatewayRoute.getId(), exchange))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalEvaluation -> optionalEvaluation
                        .map(evaluation -> applyDecision(evaluation, exchange, chain, startedAt))
                        .orElseGet(() -> chain.filter(exchange)));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> applyDecision(
            RateLimitEvaluation evaluation,
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Instant startedAt
    ) {
        evaluation.decisionOptional().ifPresent(decision -> writeRateLimitHeaders(exchange, decision));
        if (!evaluation.allowed()) {
            return block(evaluation, exchange, startedAt);
        }
        return chain.filter(exchange)
                .doFinally(signalType -> recordAsync(evaluation, exchange, startedAt, statusCode(exchange), null));
    }

    private Mono<Void> block(RateLimitEvaluation evaluation, ServerWebExchange exchange, Instant startedAt) {
        RateLimitRuntimeDecision decision = evaluation.decision();
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        recordAsync(evaluation, exchange, startedAt, HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCode.RATE_LIMIT_EXCEEDED.code());

        try {
            ErrorResponse response = ErrorResponse.of(
                    correlationId(exchange),
                    ErrorCode.RATE_LIMIT_EXCEEDED.code(),
                    "Rate limit exceeded",
                    exchange.getRequest().getPath().value(),
                    List.of()
            );
            byte[] body = objectMapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception exception) {
            return exchange.getResponse().setComplete();
        }
    }

    private void writeRateLimitHeaders(ServerWebExchange exchange, RateLimitRuntimeDecision decision) {
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(decision.effectiveLimit()));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(Math.max(0, decision.remainingTokens())));
        if (!decision.allowed()) {
            exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        }
    }

    private void recordAsync(
            RateLimitEvaluation evaluation,
            ServerWebExchange exchange,
            Instant startedAt,
            int statusCode,
            String errorCode
    ) {
        Mono.fromRunnable(() -> {
                    try {
                        rateLimitRuntimeService.recordCompletion(evaluation, exchange, startedAt, statusCode, errorCode);
                    } catch (RuntimeException exception) {
                        log.warn("Rate limit request persistence failed: routeKey={}", evaluation.route().getRouteKey(), exception);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private int statusCode(ServerWebExchange exchange) {
        return exchange.getResponse().getStatusCode() == null
                ? HttpStatus.OK.value()
                : exchange.getResponse().getStatusCode().value();
    }

    private String correlationId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return value == null ? null : value.toString();
    }
}
