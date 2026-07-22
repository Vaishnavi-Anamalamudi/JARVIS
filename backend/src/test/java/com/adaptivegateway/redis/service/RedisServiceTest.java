package com.adaptivegateway.redis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.entity.Role;
import com.adaptivegateway.auth.enums.UserStatus;
import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import com.adaptivegateway.redis.dto.RedisSessionSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void keyServiceBuildsNamespacedKeys() {
        RedisKeyService keyService = new RedisKeyService(properties());

        assertThat(keyService.build("auth", "session", "user-id"))
                .isEqualTo("agw:v1:auth:session:user-id");
    }

    @Test
    void sessionCacheWritesAndReadsJsonSnapshots() {
        RedisSessionCacheService service = new RedisSessionCacheService(
                redisTemplate,
                properties(),
                new RedisKeyService(properties()),
                new ObjectMapper().findAndRegisterModules()
        );
        AppUser user = user();
        Instant accessExpiresAt = Instant.parse("2026-07-19T16:00:00Z");
        Instant refreshExpiresAt = Instant.parse("2026-07-26T16:00:00Z");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.cacheSession(user, accessExpiresAt, refreshExpiresAt);

        verify(valueOperations).set(eq("agw:v1:auth:session:" + user.getId()), any(String.class), eq(Duration.ofMinutes(30)));

        String json = """
                {"userId":"%s","username":"codex","email":"codex@example.com","role":"ADMIN","status":"ACTIVE","accessTokenExpiresAt":"2026-07-19T16:00:00Z","refreshTokenExpiresAt":"2026-07-26T16:00:00Z","cachedAt":"2026-07-19T15:45:00Z"}
                """.formatted(user.getId()).trim();
        when(valueOperations.get("agw:v1:auth:session:" + user.getId())).thenReturn(json);

        Optional<RedisSessionSnapshot> snapshot = service.readSession(user.getId());

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().username()).isEqualTo("codex");
        assertThat(snapshot.get().role()).isEqualTo("ADMIN");
    }

    private RedisIntegrationProperties properties() {
        return new RedisIntegrationProperties("agw:v1", 30, 5);
    }

    private AppUser user() {
        Role role = new Role();
        role.setName("ADMIN");
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("codex");
        user.setEmail("codex@example.com");
        user.setFullName("Codex User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return user;
    }
}
