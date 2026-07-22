package com.adaptivegateway.kafka.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.config.ApplicationProperties;
import com.adaptivegateway.kafka.config.KafkaIntegrationProperties;
import com.adaptivegateway.kafka.dto.KafkaEventRequest;
import com.adaptivegateway.kafka.dto.KafkaOutboxEventResponse;
import com.adaptivegateway.kafka.dto.KafkaPublishSummaryResponse;
import com.adaptivegateway.kafka.entity.KafkaEventOutbox;
import com.adaptivegateway.kafka.enums.KafkaOutboxStatus;
import com.adaptivegateway.kafka.mapper.KafkaOutboxMapper;
import com.adaptivegateway.kafka.repository.KafkaEventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.KafkaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaEventPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventOutboxRepository outboxRepository;
    private final KafkaIntegrationProperties kafkaProperties;
    private final ApplicationProperties applicationProperties;
    private final KafkaOutboxMapper mapper;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisherService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaEventOutboxRepository outboxRepository,
            KafkaIntegrationProperties kafkaProperties,
            ApplicationProperties applicationProperties,
            KafkaOutboxMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.kafkaProperties = kafkaProperties;
        this.applicationProperties = applicationProperties;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KafkaOutboxEventResponse recordAndPublish(KafkaEventRequest request) {
        KafkaEventOutbox event = new KafkaEventOutbox();
        event.setEventType(requireText(request.eventType(), "Kafka event type is required"));
        event.setAggregateType(requireText(request.aggregateType(), "Kafka aggregate type is required"));
        event.setAggregateId(request.aggregateId());
        event.setCorrelationId(parseCorrelationId(request.correlationId()));
        event.setSourceService(applicationProperties.serviceName());
        event.setSchemaVersion(kafkaProperties.eventSchemaVersion());
        event.setPayload(envelope(request.payload()));
        event.setStatus(KafkaOutboxStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());

        KafkaEventOutbox saved = outboxRepository.save(event);
        publish(saved, topic(request.topic()));
        return mapper.toResponse(outboxRepository.save(saved));
    }

    @Transactional
    public KafkaPublishSummaryResponse publishDueEvents() {
        List<KafkaEventOutbox> dueEvents = outboxRepository.findDuePendingEvents(
                Instant.now(),
                PageRequest.of(0, kafkaProperties.outboxBatchSize())
        );
        int published = 0;
        int failed = 0;
        for (KafkaEventOutbox event : dueEvents) {
            KafkaOutboxStatus before = event.getStatus();
            publish(event, topicFor(event));
            if (event.getStatus() == KafkaOutboxStatus.PUBLISHED) {
                published++;
            } else if (before == KafkaOutboxStatus.PENDING && event.getStatus() != KafkaOutboxStatus.PUBLISHED) {
                failed++;
            }
        }
        outboxRepository.saveAll(dueEvents);
        return new KafkaPublishSummaryResponse(dueEvents.size(), published, failed, Instant.now());
    }

    @Scheduled(fixedDelayString = "${adaptive-gateway.kafka.outbox-poll-interval-ms}")
    public void publishDueEventsOnSchedule() {
        publishDueEvents();
    }

    private void publish(KafkaEventOutbox event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event.getPayload());
            kafkaTemplate.send(topic, event.getId().toString(), payload)
                    .get(kafkaProperties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            event.markPublished(Instant.now());
        } catch (KafkaException | JsonProcessingException exception) {
            event.markPublishFailed(Instant.now(), kafkaProperties.maxAttempts(), kafkaProperties.retryDelay());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            event.markPublishFailed(Instant.now(), kafkaProperties.maxAttempts(), kafkaProperties.retryDelay());
        } catch (Exception exception) {
            event.markPublishFailed(Instant.now(), kafkaProperties.maxAttempts(), kafkaProperties.retryDelay());
        }
    }

    private Map<String, Object> envelope(Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("schemaVersion", kafkaProperties.eventSchemaVersion());
        envelope.put("sourceService", applicationProperties.serviceName());
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("data", payload == null ? Map.of() : new LinkedHashMap<>(payload));
        return envelope;
    }

    private String topic(String requestedTopic) {
        return requestedTopic == null || requestedTopic.isBlank()
                ? kafkaProperties.defaultTopic()
                : requestedTopic.trim();
    }

    private String topicFor(KafkaEventOutbox event) {
        if ("gateway.request.completed".equals(event.getEventType())) {
            return kafkaProperties.requestEventsTopic();
        }
        return kafkaProperties.defaultTopic();
    }

    private UUID parseCorrelationId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Correlation id must be a UUID");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, message);
        }
        return value.trim();
    }
}
