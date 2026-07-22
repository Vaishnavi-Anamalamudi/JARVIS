package com.adaptivegateway.consumer.mapper;

import com.adaptivegateway.consumer.dto.ApiConsumerResponse;
import com.adaptivegateway.consumer.dto.ApiCredentialResponse;
import com.adaptivegateway.consumer.entity.ApiConsumer;
import com.adaptivegateway.consumer.entity.ApiConsumerCredential;
import org.springframework.stereotype.Component;

@Component
public class ApiConsumerMapper {

    public ApiConsumerResponse toResponse(ApiConsumer consumer) {
        return new ApiConsumerResponse(
                consumer.getId(),
                consumer.getOwnerUser().getId(),
                consumer.getOwnerUser().getUsername(),
                consumer.getName(),
                consumer.getDescription(),
                consumer.getContactEmail(),
                consumer.getStatus(),
                consumer.getEnvironment(),
                consumer.getCreatedAt(),
                consumer.getUpdatedAt()
        );
    }

    public ApiCredentialResponse toResponse(ApiConsumerCredential credential) {
        return new ApiCredentialResponse(
                credential.getId(),
                credential.getConsumer().getId(),
                credential.getKeyPrefix(),
                credential.getExpiresAt(),
                credential.getRevokedAt(),
                credential.getLastUsedAt(),
                credential.getCreatedAt(),
                credential.getUpdatedAt()
        );
    }
}
