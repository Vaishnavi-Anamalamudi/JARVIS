package com.adaptivegateway.redis.service;

import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class RedisKeyService {

    private final RedisIntegrationProperties properties;

    public RedisKeyService(RedisIntegrationProperties properties) {
        this.properties = properties;
    }

    public String build(String domain, String... parts) {
        StringBuilder key = new StringBuilder(clean(properties.keyPrefix())).append(":").append(clean(domain));
        Arrays.stream(parts)
                .map(this::clean)
                .forEach(part -> key.append(":").append(part));
        return key.toString();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Redis key parts must not be blank");
        }
        String cleaned = value.trim().replaceAll("[^A-Za-z0-9:_-]", "_");
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Redis key parts must contain valid characters");
        }
        return cleaned;
    }
}
