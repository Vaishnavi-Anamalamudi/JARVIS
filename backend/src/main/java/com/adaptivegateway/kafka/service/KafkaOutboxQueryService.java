package com.adaptivegateway.kafka.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.kafka.dto.KafkaOutboxEventResponse;
import com.adaptivegateway.kafka.enums.KafkaOutboxStatus;
import com.adaptivegateway.kafka.mapper.KafkaOutboxMapper;
import com.adaptivegateway.kafka.repository.KafkaEventOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaOutboxQueryService {

    private static final List<String> OUTBOX_SORTS = List.of("createdAt", "updatedAt", "eventType", "status", "attempts");

    private final KafkaEventOutboxRepository outboxRepository;
    private final KafkaOutboxMapper mapper;

    public KafkaOutboxQueryService(KafkaEventOutboxRepository outboxRepository, KafkaOutboxMapper mapper) {
        this.outboxRepository = outboxRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<KafkaOutboxEventResponse> listEvents(
            KafkaOutboxStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction);
        var events = status == null
                ? outboxRepository.findByDeletedAtIsNull(pageRequest)
                : outboxRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        return PageResponse.from(events.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public KafkaOutboxEventResponse getEvent(UUID id) {
        return outboxRepository.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Kafka outbox event does not exist"));
    }

    private PageRequest pageRequest(int page, int size, String sort, String direction) {
        String sortProperty = OUTBOX_SORTS.contains(sort) ? sort : "createdAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
