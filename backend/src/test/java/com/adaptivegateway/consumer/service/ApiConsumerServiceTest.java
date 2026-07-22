package com.adaptivegateway.consumer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.entity.Role;
import com.adaptivegateway.auth.enums.UserStatus;
import com.adaptivegateway.auth.repository.AppUserRepository;
import com.adaptivegateway.consumer.dto.CreateCredentialRequest;
import com.adaptivegateway.consumer.entity.ApiConsumer;
import com.adaptivegateway.consumer.entity.ApiConsumerCredential;
import com.adaptivegateway.consumer.enums.ApiConsumerEnvironment;
import com.adaptivegateway.consumer.enums.ApiConsumerStatus;
import com.adaptivegateway.consumer.mapper.ApiConsumerMapper;
import com.adaptivegateway.consumer.repository.ApiConsumerCredentialRepository;
import com.adaptivegateway.consumer.repository.ApiConsumerRepository;
import com.adaptivegateway.operations.service.AuditLogService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

@ExtendWith(MockitoExtension.class)
class ApiConsumerServiceTest {

    @Mock
    private ApiConsumerRepository consumerRepository;

    @Mock
    private ApiConsumerCredentialRepository credentialRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServerWebExchange exchange;

    private ApiConsumerService service;

    @BeforeEach
    void setUp() {
        service = new ApiConsumerService(
                consumerRepository,
                credentialRepository,
                appUserRepository,
                new ApiConsumerMapper(),
                auditLogService
        );
    }

    @Test
    void createCredentialReturnsRawKeyOnceAndStoresOnlyHash() {
        ApiConsumer consumer = consumer();
        when(consumerRepository.findByIdAndDeletedAtIsNull(consumer.getId())).thenReturn(Optional.of(consumer));
        when(credentialRepository.existsByKeyPrefixAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(credentialRepository.save(any(ApiConsumerCredential.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var response = service.createCredential(
                consumer.getId(),
                new CreateCredentialRequest(Instant.now().plusSeconds(3600)),
                jwt(),
                exchange
        );

        assertThat(response.apiKey()).startsWith("agw_");
        assertThat(response.credential().keyPrefix()).isEqualTo(response.apiKey().substring(0, 12));

        ArgumentCaptor<ApiConsumerCredential> credentialCaptor = ArgumentCaptor.forClass(ApiConsumerCredential.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getCredentialHash()).isNotEqualTo(response.apiKey());
        assertThat(credentialCaptor.getValue().getCredentialHash()).hasSize(64);
    }

    private static ApiConsumer consumer() {
        AppUser owner = new AppUser();
        Role role = new Role();
        role.setName("ADMIN");
        owner.setRole(role);
        owner.setUsername("admin");
        owner.setEmail("admin@example.com");
        owner.setFullName("Admin User");
        owner.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());

        ApiConsumer consumer = new ApiConsumer();
        ReflectionTestUtils.setField(consumer, "id", UUID.randomUUID());
        consumer.setOwnerUser(owner);
        consumer.setName("Orders client");
        consumer.setContactEmail("orders@example.com");
        consumer.setStatus(ApiConsumerStatus.ACTIVE);
        consumer.setEnvironment(ApiConsumerEnvironment.DEVELOPMENT);
        return consumer;
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .build();
    }

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
