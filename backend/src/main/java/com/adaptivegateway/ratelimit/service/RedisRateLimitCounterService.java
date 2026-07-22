package com.adaptivegateway.ratelimit.service;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.ratelimit.dto.RedisRateLimitResult;
import com.adaptivegateway.redis.service.RedisKeyService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimitCounterService {

    private static final String TOKEN_BUCKET_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local refill_tokens = tonumber(ARGV[3])
            local refill_period_ms = tonumber(ARGV[4])
            local ttl_seconds = tonumber(ARGV[5])
            local current_tokens = tonumber(redis.call('HGET', key, 'tokens'))
            local last_refill = tonumber(redis.call('HGET', key, 'lastRefill'))
            if current_tokens == nil then
                current_tokens = capacity
                last_refill = now
            end
            local elapsed = now - last_refill
            if elapsed >= refill_period_ms then
                local periods = math.floor(elapsed / refill_period_ms)
                current_tokens = math.min(capacity, current_tokens + (periods * refill_tokens))
                last_refill = last_refill + (periods * refill_period_ms)
            end
            local allowed = 0
            if current_tokens >= 1 then
                allowed = 1
                current_tokens = current_tokens - 1
            end
            redis.call('HSET', key, 'tokens', current_tokens, 'lastRefill', last_refill)
            redis.call('EXPIRE', key, ttl_seconds)
            local retry_after = 0
            if allowed == 0 then
                retry_after = math.max(1, math.ceil((refill_period_ms - (now - last_refill)) / 1000))
            end
            return {allowed, current_tokens, retry_after}
            """;

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyService redisKeyService;
    private final DefaultRedisScript<List> tokenBucketScript = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, List.class);

    public RedisRateLimitCounterService(StringRedisTemplate redisTemplate, RedisKeyService redisKeyService) {
        this.redisTemplate = redisTemplate;
        this.redisKeyService = redisKeyService;
    }

    public RedisRateLimitResult slidingWindow(UUID routeId, String identifier, int limit, int windowSeconds) {
        String key = redisKeyService.build("rate-limit", "sliding", routeId.toString(), identifier);
        long nowMillis = Instant.now().toEpochMilli();
        long startMillis = nowMillis - Duration.ofSeconds(windowSeconds).toMillis();
        String member = nowMillis + ":" + UUID.randomUUID();
        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, startMillis);
            redisTemplate.opsForZSet().add(key, member, nowMillis);
            Long count = redisTemplate.opsForZSet().zCard(key);
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 1L));
            int observedCount = count == null ? 0 : count.intValue();
            boolean allowed = observedCount <= limit;
            return new RedisRateLimitResult(allowed, observedCount, Math.max(0, limit - observedCount), windowSeconds);
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis rate limit counter update failed");
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis rate limit counter update failed");
        }
    }

    public RedisRateLimitResult tokenBucket(
            UUID routeId,
            String identifier,
            int capacity,
            int refillTokens,
            int refillPeriodSeconds
    ) {
        String key = redisKeyService.build("rate-limit", "token-bucket", routeId.toString(), identifier);
        try {
            List<?> result = redisTemplate.execute(
                    tokenBucketScript,
                    List.of(key),
                    String.valueOf(Instant.now().toEpochMilli()),
                    String.valueOf(capacity),
                    String.valueOf(refillTokens),
                    String.valueOf(Duration.ofSeconds(refillPeriodSeconds).toMillis()),
                    String.valueOf(Math.max(refillPeriodSeconds * 2L, 60L))
            );
            if (result == null || result.size() < 3) {
                throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis token bucket result is invalid");
            }
            boolean allowed = toInt(result.get(0)) == 1;
            int remainingTokens = toInt(result.get(1));
            int retryAfterSeconds = toInt(result.get(2));
            return new RedisRateLimitResult(allowed, 0, remainingTokens, retryAfterSeconds);
        } catch (RedisConnectionFailureException exception) {
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis rate limit token bucket update failed");
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, "Redis rate limit token bucket update failed");
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
