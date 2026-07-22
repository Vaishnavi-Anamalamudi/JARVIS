package com.adaptivegateway.redis.service;

import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import com.adaptivegateway.redis.dto.RedisSessionSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSessionCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedisIntegrationProperties properties;
    private final RedisKeyService redisKeyService;
    private final ObjectMapper objectMapper;

    public RedisSessionCacheService(
            StringRedisTemplate redisTemplate,
            RedisIntegrationProperties properties,
            RedisKeyService redisKeyService,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.redisKeyService = redisKeyService;
        this.objectMapper = objectMapper;
    }

    public void cacheSession(AppUser user, Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {
        RedisSessionSnapshot snapshot = new RedisSessionSnapshot(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName(),
                user.getStatus().name(),
                user.getLastLoginAt(),
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                Instant.now()
        );
        write(key(user.getId()), toJson(snapshot));
    }

    public Optional<RedisSessionSnapshot> readSession(UUID userId) {
        try {
            String json = redisTemplate.opsForValue().get(key(userId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, RedisSessionSnapshot.class));
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis session cache read failed");
        } catch (RuntimeException | JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis session cache read failed");
        }
    }

    public void evictSession(UUID userId) {
        try {
            redisTemplate.delete(key(userId));
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis session cache eviction failed");
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis session cache eviction failed");
        }
    }

    private void write(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, properties.sessionCacheTtl());
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis session cache update failed");
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis session cache update failed");
        }
    }

    private String key(UUID userId) {
        return redisKeyService.build("auth", "session", userId.toString());
    }

    private String toJson(RedisSessionSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Redis session cache serialization failed");
        }
    }
}
