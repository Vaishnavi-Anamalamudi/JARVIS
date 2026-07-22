package com.adaptivegateway.foundation.controller;

import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import com.adaptivegateway.config.ApplicationProperties;
import com.adaptivegateway.foundation.dto.DatabaseHealthResponse;
import com.adaptivegateway.foundation.dto.SystemHealthResponse;
import com.adaptivegateway.kafka.service.KafkaHealthService;
import com.adaptivegateway.redis.service.RedisHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/system")
@Tag(name = "System")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationProperties properties;
    private final RedisHealthService redisHealthService;
    private final KafkaHealthService kafkaHealthService;

    public SystemHealthController(
            JdbcTemplate jdbcTemplate,
            ApplicationProperties properties,
            RedisHealthService redisHealthService,
            KafkaHealthService kafkaHealthService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.redisHealthService = redisHealthService;
        this.kafkaHealthService = kafkaHealthService;
    }

    @GetMapping("/health")
    @Operation(summary = "Read backend, PostgreSQL, Redis, and Kafka health")
    public ApiResponse<SystemHealthResponse> health(
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        DatabaseHealthResponse database = readDatabaseHealth();
        SystemHealthResponse response = new SystemHealthResponse(
                properties.serviceName(),
                properties.apiVersion(),
                "UP",
                database,
                redisHealthService.readHealth(),
                kafkaHealthService.readHealth(correlationId),
                Instant.now()
        );
        return ApiResponse.success(correlationId, "System health read successfully", response);
    }

    private DatabaseHealthResponse readDatabaseHealth() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT current_database(), current_schema(), version()",
                    (resultSet, rowNumber) -> new DatabaseHealthResponse(
                            "UP",
                            resultSet.getString(1),
                            resultSet.getString(2),
                            resultSet.getString(3)
                    )
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.DATABASE_UNAVAILABLE, "PostgreSQL health check failed");
        }
    }
}
