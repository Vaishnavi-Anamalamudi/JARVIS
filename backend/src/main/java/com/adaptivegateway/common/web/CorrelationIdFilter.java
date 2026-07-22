package com.adaptivegateway.common.web;

import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest());
        exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange)
                .contextWrite(context -> context.put(CORRELATION_ID_ATTRIBUTE, correlationId))
                .doOnEach(signal -> {
                    if (signal.isOnNext() || signal.isOnComplete()) {
                        MDC.put(CORRELATION_ID_ATTRIBUTE, correlationId);
                    }
                })
                .doFinally(signalType -> MDC.remove(CORRELATION_ID_ATTRIBUTE));
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        List<String> values = request.getHeaders().get(CORRELATION_ID_HEADER);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            return UUID.randomUUID().toString();
        }
        return values.getFirst();
    }
}
