package com.adaptivegateway.kafka.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.config.ApplicationProperties;
import com.adaptivegateway.kafka.config.KafkaIntegrationProperties;
import com.adaptivegateway.kafka.dto.KafkaHealthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaHealthService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaIntegrationProperties kafkaProperties;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    public KafkaHealthService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaIntegrationProperties kafkaProperties,
            ApplicationProperties applicationProperties,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    public KafkaHealthResponse readHealth(String correlationId) {
        Instant started = Instant.now();
        try {
            String eventId = UUID.randomUUID().toString();
            kafkaTemplate.send(kafkaProperties.healthTopic(), eventId, objectMapper.writeValueAsString(payload(eventId, correlationId)))
                    .get(kafkaProperties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            long roundTripMillis = Duration.between(started, Instant.now()).toMillis();
            return new KafkaHealthResponse("UP", kafkaProperties.healthTopic(), roundTripMillis, Instant.now());
        } catch (KafkaException exception) {
            throw new BusinessException(ErrorCode.KAFKA_UNAVAILABLE, "Kafka health check failed");
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(ErrorCode.KAFKA_UNAVAILABLE, "Kafka health check failed");
        }
    }

    private Map<String, Object> payload(String eventId, String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "system.kafka.health");
        payload.put("eventId", eventId);
        payload.put("correlationId", correlationId);
        payload.put("sourceService", applicationProperties.serviceName());
        payload.put("schemaVersion", kafkaProperties.eventSchemaVersion());
        payload.put("checkedAt", Instant.now().toString());
        return payload;
    }
}
