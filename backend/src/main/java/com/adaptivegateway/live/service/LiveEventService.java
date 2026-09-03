package com.adaptivegateway.live.service;

import com.adaptivegateway.live.dto.LiveEventResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class LiveEventService {

    private static final Logger log = LoggerFactory.getLogger(LiveEventService.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);

    private final Sinks.Many<LiveEventResponse> sink = Sinks.many().multicast().directBestEffort();

    public Flux<LiveEventResponse> stream() {
        Flux<LiveEventResponse> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
                .map(ignored -> event(
                        "gateway.live.heartbeat",
                        "system",
                        null,
                        null,
                        "Live gateway stream heartbeat",
                        Map.of()
                ));
        return Flux.merge(sink.asFlux(), heartbeat);
    }

    public LiveEventResponse emit(
            String type,
            String resourceType,
            UUID resourceId,
            UUID correlationId,
            String message,
            Map<String, Object> payload
    ) {
        LiveEventResponse event = event(type, resourceType, resourceId, correlationId, message, payload);
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.debug("Live event was not delivered immediately: type={} resourceType={} result={}", type, resourceType, result);
        }
        return event;
    }

    private LiveEventResponse event(
            String type,
            String resourceType,
            UUID resourceId,
            UUID correlationId,
            String message,
            Map<String, Object> payload
    ) {
        return new LiveEventResponse(
                UUID.randomUUID(),
                type,
                resourceType,
                resourceId,
                correlationId,
                message,
                payload == null ? Map.of() : new LinkedHashMap<>(payload),
                Instant.now()
        );
    }
}
