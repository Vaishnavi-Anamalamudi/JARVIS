package com.adaptivegateway.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DatabaseHealthResponse(
        @Schema(example = "UP") String status,
        String databaseName,
        String schemaName,
        String version
) {
}
