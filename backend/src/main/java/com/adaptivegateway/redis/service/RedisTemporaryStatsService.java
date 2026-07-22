package com.adaptivegateway.redis.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import com.adaptivegateway.redis.dto.TemporaryStatSnapshot;
import java.time.Instant;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTemporaryStatsService {

    private final StringRedisTemplate redisTemplate;
    private final RedisIntegrationProperties properties;
    private final RedisKeyService redisKeyService;

    public RedisTemporaryStatsService(
            StringRedisTemplate redisTemplate,
            RedisIntegrationProperties properties,
            RedisKeyService redisKeyService
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.redisKeyService = redisKeyService;
    }

    public TemporaryStatSnapshot increment(String domain, String metric, String identifier) {
        String key = redisKeyService.build("stats", domain, metric, identifier);
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, properties.temporaryStatsTtl());
            Long ttlSeconds = redisTemplate.getExpire(key);
            return new TemporaryStatSnapshot(key, value == null ? 0 : value, ttlSeconds == null ? -1 : ttlSeconds, Instant.now());
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis temporary stats update failed");
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis temporary stats update failed");
        }
    }

    public TemporaryStatSnapshot read(String domain, String metric, String identifier) {
        String key = redisKeyService.build("stats", domain, metric, identifier);
        try {
            String value = redisTemplate.opsForValue().get(key);
            Long ttlSeconds = redisTemplate.getExpire(key);
            long count = value == null ? 0 : Long.parseLong(value);
            return new TemporaryStatSnapshot(key, count, ttlSeconds == null ? -1 : ttlSeconds, Instant.now());
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis temporary stats read failed");
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis temporary stats read failed");
        }
    }
}
