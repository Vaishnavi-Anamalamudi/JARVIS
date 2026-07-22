package com.adaptivegateway.consumer.service;

import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.repository.AppUserRepository;
import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.common.pagination.PageResponse;
import com.adaptivegateway.consumer.dto.ApiConsumerRequest;
import com.adaptivegateway.consumer.dto.ApiConsumerResponse;
import com.adaptivegateway.consumer.dto.ApiCredentialResponse;
import com.adaptivegateway.consumer.dto.CreateCredentialRequest;
import com.adaptivegateway.consumer.dto.CreateCredentialResponse;
import com.adaptivegateway.consumer.entity.ApiConsumer;
import com.adaptivegateway.consumer.entity.ApiConsumerCredential;
import com.adaptivegateway.consumer.enums.ApiConsumerStatus;
import com.adaptivegateway.consumer.mapper.ApiConsumerMapper;
import com.adaptivegateway.consumer.repository.ApiConsumerCredentialRepository;
import com.adaptivegateway.consumer.repository.ApiConsumerRepository;
import com.adaptivegateway.operations.service.AuditLogService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ServerWebExchange;

@Service
public class ApiConsumerService {

    private static final List<String> CONSUMER_SORTS = List.of("createdAt", "updatedAt", "name", "status", "environment");
    private final SecureRandom secureRandom = new SecureRandom();

    private final ApiConsumerRepository consumerRepository;
    private final ApiConsumerCredentialRepository credentialRepository;
    private final AppUserRepository appUserRepository;
    private final ApiConsumerMapper mapper;
    private final AuditLogService auditLogService;

    public ApiConsumerService(
            ApiConsumerRepository consumerRepository,
            ApiConsumerCredentialRepository credentialRepository,
            AppUserRepository appUserRepository,
            ApiConsumerMapper mapper,
            AuditLogService auditLogService
    ) {
        this.consumerRepository = consumerRepository;
        this.credentialRepository = credentialRepository;
        this.appUserRepository = appUserRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApiConsumerResponse> listConsumers(
            ApiConsumerStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        PageRequest pageRequest = pageRequest(page, size, sort, direction, CONSUMER_SORTS, "createdAt");
        var consumers = status == null
                ? consumerRepository.findByDeletedAtIsNull(pageRequest)
                : consumerRepository.findByStatusAndDeletedAtIsNull(status, pageRequest);
        return PageResponse.from(consumers.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ApiConsumerResponse getConsumer(UUID id) {
        return mapper.toResponse(findConsumer(id));
    }

    @Transactional
    public ApiConsumerResponse createConsumer(ApiConsumerRequest request, Jwt actor, ServerWebExchange exchange) {
        AppUser owner = appUserRepository.findActiveById(request.ownerUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Owner user does not exist"));
        String name = request.name().trim();
        if (consumerRepository.existsByOwnerUserIdAndNameIgnoreCaseAndEnvironmentAndDeletedAtIsNull(
                owner.getId(),
                name,
                request.environment()
        )) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "API consumer name is already registered for this owner and environment");
        }
        ApiConsumer consumer = new ApiConsumer();
        apply(request, owner, consumer);
        ApiConsumer saved = consumerRepository.save(consumer);
        auditLogService.record(actor.getSubject(), "API_CONSUMER_CREATED", "api_consumer", saved.getId(), exchange, Map.of("name", saved.getName()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public ApiConsumerResponse updateConsumer(UUID id, ApiConsumerRequest request, Jwt actor, ServerWebExchange exchange) {
        ApiConsumer consumer = findConsumer(id);
        AppUser owner = appUserRepository.findActiveById(request.ownerUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Owner user does not exist"));
        String name = request.name().trim();
        if ((!consumer.getName().equalsIgnoreCase(name)
                || !consumer.getOwnerUser().getId().equals(owner.getId())
                || consumer.getEnvironment() != request.environment())
                && consumerRepository.existsByOwnerUserIdAndNameIgnoreCaseAndEnvironmentAndDeletedAtIsNull(
                owner.getId(),
                name,
                request.environment()
        )) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "API consumer name is already registered for this owner and environment");
        }
        apply(request, owner, consumer);
        ApiConsumer saved = consumerRepository.save(consumer);
        auditLogService.record(actor.getSubject(), "API_CONSUMER_UPDATED", "api_consumer", saved.getId(), exchange, Map.of("name", saved.getName()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteConsumer(UUID id, Jwt actor, ServerWebExchange exchange) {
        ApiConsumer consumer = findConsumer(id);
        consumer.setDeletedAt(Instant.now());
        consumerRepository.save(consumer);
        auditLogService.record(actor.getSubject(), "API_CONSUMER_DELETED", "api_consumer", id, exchange, Map.of("name", consumer.getName()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApiCredentialResponse> listCredentials(UUID consumerId, int page, int size) {
        findConsumer(consumerId);
        return PageResponse.from(credentialRepository
                .findByConsumerIdAndDeletedAtIsNull(consumerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(mapper::toResponse));
    }

    @Transactional
    public CreateCredentialResponse createCredential(UUID consumerId, CreateCredentialRequest request, Jwt actor, ServerWebExchange exchange) {
        ApiConsumer consumer = findConsumer(consumerId);
        if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Credential expiration must be in the future");
        }
        String apiKey = generateApiKey();
        String keyPrefix = apiKey.substring(0, 12);
        while (credentialRepository.existsByKeyPrefixAndDeletedAtIsNull(keyPrefix)) {
            apiKey = generateApiKey();
            keyPrefix = apiKey.substring(0, 12);
        }
        ApiConsumerCredential credential = new ApiConsumerCredential();
        credential.setConsumer(consumer);
        credential.setKeyPrefix(keyPrefix);
        credential.setCredentialHash(sha256(apiKey));
        credential.setExpiresAt(request.expiresAt());
        ApiConsumerCredential saved = credentialRepository.save(credential);
        auditLogService.record(actor.getSubject(), "API_CREDENTIAL_CREATED", "api_consumer_credential", saved.getId(), exchange, Map.of("consumerId", consumerId.toString()));
        return new CreateCredentialResponse(mapper.toResponse(saved), apiKey);
    }

    @Transactional
    public ApiCredentialResponse revokeCredential(UUID credentialId, Jwt actor, ServerWebExchange exchange) {
        ApiConsumerCredential credential = credentialRepository.findByIdAndDeletedAtIsNull(credentialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "API credential does not exist"));
        if (credential.getRevokedAt() == null) {
            credential.setRevokedAt(Instant.now());
        }
        ApiConsumerCredential saved = credentialRepository.save(credential);
        auditLogService.record(actor.getSubject(), "API_CREDENTIAL_REVOKED", "api_consumer_credential", saved.getId(), exchange, Map.of("consumerId", saved.getConsumer().getId().toString()));
        return mapper.toResponse(saved);
    }

    private void apply(ApiConsumerRequest request, AppUser owner, ApiConsumer consumer) {
        consumer.setOwnerUser(owner);
        consumer.setName(request.name().trim());
        consumer.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        consumer.setContactEmail(request.contactEmail().trim().toLowerCase(Locale.ROOT));
        consumer.setStatus(request.status());
        consumer.setEnvironment(request.environment());
    }

    private ApiConsumer findConsumer(UUID id) {
        return consumerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "API consumer does not exist"));
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "agw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private PageRequest pageRequest(
            int page,
            int size,
            String sort,
            String direction,
            List<String> allowedSorts,
            String defaultSort
    ) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
