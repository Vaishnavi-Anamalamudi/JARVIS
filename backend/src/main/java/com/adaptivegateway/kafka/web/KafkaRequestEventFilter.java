package com.adaptivegateway.kafka.web;

import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.kafka.config.KafkaIntegrationProperties;
import com.adaptivegateway.kafka.dto.KafkaEventRequest;
import com.adaptivegateway.kafka.service.KafkaEventPublisherService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Order(200)
public class KafkaRequestEventFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(KafkaRequestEventFilter.class);

    private final KafkaEventPublisherService publisherService;
    private final KafkaIntegrationProperties kafkaProperties;

    public KafkaRequestEventFilter(KafkaEventPublisherService publisherService, KafkaIntegrationProperties kafkaProperties) {
        this.publisherService = publisherService;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant started = Instant.now();
        return chain.filter(exchange)
                .doFinally(signalType -> Mono.fromRunnable(() -> publish(exchange, started))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe());
    }

    private void publish(ServerWebExchange exchange, Instant started) {
        Map<String, Object> payload = new LinkedHashMap<>();
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode == null ? 200 : statusCode.value();
        payload.put("method", exchange.getRequest().getMethod().name());
        payload.put("path", exchange.getRequest().getPath().value());
        payload.put("statusCode", status);
        payload.put("outcome", outcome(status));
        payload.put("durationMs", Duration.between(started, Instant.now()).toMillis());
        payload.put("sourceIp", sourceIp(exchange));
        payload.put("userAgent", exchange.getRequest().getHeaders().getFirst("User-Agent"));

        Object correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        try {
            publisherService.recordAndPublish(new KafkaEventRequest(
                    kafkaProperties.requestEventsTopic(),
                    "gateway.request.completed",
                    "http_request",
                    null,
                    correlationId == null ? null : correlationId.toString(),
                    payload
            ));
        } catch (RuntimeException exception) {
            log.warn("Request Kafka event publication failed: path={}", exchange.getRequest().getPath().value(), exception);
        }
    }

    private String outcome(int status) {
        if (status == 429 || status == 401 || status == 403) {
            return "BLOCKED";
        }
        return status >= 500 ? "ERROR" : "ALLOWED";
    }

    private String sourceIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null) {
            return null;
        }
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
