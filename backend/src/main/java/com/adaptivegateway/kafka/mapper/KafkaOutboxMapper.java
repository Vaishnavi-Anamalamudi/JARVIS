package com.adaptivegateway.kafka.mapper;

import com.adaptivegateway.kafka.dto.KafkaOutboxEventResponse;
import com.adaptivegateway.kafka.entity.KafkaEventOutbox;
import org.springframework.stereotype.Component;

@Component
public class KafkaOutboxMapper {

    public KafkaOutboxEventResponse toResponse(KafkaEventOutbox event) {
        return new KafkaOutboxEventResponse(
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getCorrelationId(),
                event.getSourceService(),
                event.getSchemaVersion(),
                event.getPayload(),
                event.getStatus(),
                event.getAttempts(),
                event.getNextAttemptAt(),
                event.getPublishedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
