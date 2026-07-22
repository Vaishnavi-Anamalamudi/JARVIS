package com.adaptivegateway.gateway.dto;

import com.adaptivegateway.gateway.enums.UpstreamServiceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpstreamServiceRequest(
        @NotBlank @Size(max = 140) String name,
        @NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://.+") String baseUrl,
        @Size(max = 512) @Pattern(regexp = "^/.*", message = "must start with /") String healthCheckPath,
        @NotNull UpstreamServiceStatus status,
        @NotNull @Min(100) @Max(120000) Integer timeoutMs,
        @NotNull @Min(0) @Max(10) Integer retryCount
) {
}
