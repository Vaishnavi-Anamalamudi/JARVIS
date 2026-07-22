package com.adaptivegateway.consumer.dto;

import com.adaptivegateway.consumer.enums.ApiConsumerEnvironment;
import com.adaptivegateway.consumer.enums.ApiConsumerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ApiConsumerRequest(
        @NotNull UUID ownerUserId,
        @NotBlank @Size(max = 140) String name,
        @Size(max = 1000) String description,
        @NotBlank @Email @Size(max = 254) String contactEmail,
        @NotNull ApiConsumerStatus status,
        @NotNull ApiConsumerEnvironment environment
) {
}
