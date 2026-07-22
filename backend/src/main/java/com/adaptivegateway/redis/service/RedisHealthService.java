package com.adaptivegateway.redis.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import com.adaptivegateway.redis.dto.RedisHealthResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisHealthService {

    private final StringRedisTemplate redisTemplate;
    private final RedisIntegrationProperties properties;
    private final RedisKeyService redisKeyService;

    public RedisHealthService(
            StringRedisTemplate redisTemplate,
            RedisIntegrationProperties properties,
            RedisKeyService redisKeyService
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.redisKeyService = redisKeyService;
    }

    public RedisHealthResponse readHealth() {
        String key = redisKeyService.build("system", "health");
        String value = Instant.now().toString();
        Instant started = Instant.now();
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(30));
            String stored = redisTemplate.opsForValue().get(key);
            if (!value.equals(stored)) {
                throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis health check failed");
            }
            long roundTripMillis = Duration.between(started, Instant.now()).toMillis();
            return new RedisHealthResponse("UP", properties.keyPrefix(), roundTripMillis, Instant.now());
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis health check failed");
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis health check failed");
        }
    }
}
