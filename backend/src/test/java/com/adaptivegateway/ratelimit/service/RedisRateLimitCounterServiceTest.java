package com.adaptivegateway.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import com.adaptivegateway.redis.service.RedisKeyService;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RedisRateLimitCounterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisRateLimitCounterService service;

    @BeforeEach
    void setUp() {
        service = new RedisRateLimitCounterService(
                redisTemplate,
                new RedisKeyService(new RedisIntegrationProperties("agw:v1", 30, 5))
        );
    }

    @Test
    void slidingWindowAllowsWhenObservedCountIsWithinLimit() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(5L);

        var result = service.slidingWindow(UUID.randomUUID(), "route:127.0.0.1", 10, 60);

        assertThat(result.allowed()).isTrue();
        assertThat(result.observedCount()).isEqualTo(5);
        assertThat(result.remainingTokens()).isEqualTo(5);
    }

    @Test
    void slidingWindowBlocksWhenObservedCountExceedsLimit() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(11L);

        var result = service.slidingWindow(UUID.randomUUID(), "route:127.0.0.1", 10, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.observedCount()).isEqualTo(11);
        assertThat(result.remainingTokens()).isZero();
        org.mockito.Mockito.verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(61)));
        org.mockito.Mockito.verify(zSetOperations).removeRangeByScore(anyString(), eq(0.0), anyDouble());
    }
}
