package com.adaptivegateway.kafka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.config.ApplicationProperties;
import com.adaptivegateway.kafka.config.KafkaIntegrationProperties;
import com.adaptivegateway.kafka.dto.KafkaEventRequest;
import com.adaptivegateway.kafka.entity.KafkaEventOutbox;
import com.adaptivegateway.kafka.enums.KafkaOutboxStatus;
import com.adaptivegateway.kafka.mapper.KafkaOutboxMapper;
import com.adaptivegateway.kafka.repository.KafkaEventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaEventOutboxRepository outboxRepository;

    private KafkaEventPublisherService service;

    @BeforeEach
    void setUp() {
        service = new KafkaEventPublisherService(
                kafkaTemplate,
                outboxRepository,
                properties(),
                new ApplicationProperties("adaptive-api-gateway", "v1"),
                new KafkaOutboxMapper(),
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void recordAndPublishPersistsOutboxAndMarksPublishedWhenKafkaSendSucceeds() {
        when(outboxRepository.save(any(KafkaEventOutbox.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(kafkaTemplate.send(eq("gateway.requests"), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        var response = service.recordAndPublish(new KafkaEventRequest(
                "gateway.requests",
                "gateway.request.completed",
                "http_request",
                null,
                UUID.randomUUID().toString(),
                Map.of("path", "/api/system/health", "statusCode", 200)
        ));

        assertThat(response.status()).isEqualTo(KafkaOutboxStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
        assertThat(response.payload()).containsKeys("schemaVersion", "sourceService", "occurredAt", "data");
        verify(kafkaTemplate).send(eq("gateway.requests"), any(String.class), any(String.class));
    }

    @Test
    void recordAndPublishKeepsOutboxPendingWhenKafkaSendFailsBeforeMaxAttempts() {
        when(outboxRepository.save(any(KafkaEventOutbox.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(kafkaTemplate.send(eq("gateway.requests"), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker unavailable")));

        var response = service.recordAndPublish(new KafkaEventRequest(
                "gateway.requests",
                "gateway.request.completed",
                "http_request",
                null,
                UUID.randomUUID().toString(),
                Map.of("path", "/api/auth/login", "statusCode", 401)
        ));

        assertThat(response.status()).isEqualTo(KafkaOutboxStatus.PENDING);
        assertThat(response.attempts()).isEqualTo(1);
        assertThat(response.nextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void publishDueEventsUsesRequestEventsTopicForRequestCompletedRetries() {
        KafkaEventOutbox event = withId(new KafkaEventOutbox());
        event.setEventType("gateway.request.completed");
        event.setAggregateType("http_request");
        event.setSourceService("adaptive-api-gateway");
        event.setSchemaVersion(1);
        event.setPayload(Map.of("data", Map.of("path", "/api/gateway/routes")));
        event.setStatus(KafkaOutboxStatus.PENDING);
        event.setAttempts(1);
        event.setNextAttemptAt(Instant.now().minusSeconds(5));

        when(outboxRepository.findDuePendingEvents(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("gateway.requests"), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        var response = service.publishDueEvents();

        assertThat(response.attempted()).isEqualTo(1);
        assertThat(response.published()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(KafkaOutboxStatus.PUBLISHED);
        verify(kafkaTemplate).send(eq("gateway.requests"), any(String.class), any(String.class));
    }

    private static KafkaIntegrationProperties properties() {
        return new KafkaIntegrationProperties(
                "gateway.events",
                "gateway.requests",
                "gateway.health",
                1,
                1000,
                30,
                3,
                50,
                10000
        );
    }

    private static SendResult<String, String> sendResult() {
        return new SendResult<>(new ProducerRecord<>("gateway.requests", "key", "value"), null);
    }

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
