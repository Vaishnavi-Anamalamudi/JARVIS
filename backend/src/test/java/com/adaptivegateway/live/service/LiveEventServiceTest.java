package com.adaptivegateway.live.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

class LiveEventServiceTest {

    @Test
    void streamPublishesEmittedGatewayEvent() throws InterruptedException {
        LiveEventService service = new LiveEventService();
        UUID resourceId = UUID.randomUUID();
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicReference<com.adaptivegateway.live.dto.LiveEventResponse> received = new AtomicReference<>();

        Disposable subscription = service.stream().subscribe(event -> {
            received.set(event);
            delivered.countDown();
        });

        service.emit(
                "gateway.request.completed",
                "request_log",
                resourceId,
                null,
                "Gateway request completed",
                Map.of("routeKey", "orders")
        );

        assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
        subscription.dispose();
        assertThat(received.get().type()).isEqualTo("gateway.request.completed");
        assertThat(received.get().resourceType()).isEqualTo("request_log");
        assertThat(received.get().resourceId()).isEqualTo(resourceId);
        assertThat(received.get().payload()).containsEntry("routeKey", "orders");
    }
}
