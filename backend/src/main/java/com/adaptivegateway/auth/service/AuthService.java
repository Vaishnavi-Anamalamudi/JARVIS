package com.adaptivegateway.auth.service;

import com.adaptivegateway.auth.controller.AuthController.AuthRequestContext;
import com.adaptivegateway.auth.dto.AuthResponse;
import com.adaptivegateway.auth.dto.CurrentUserResponse;
import com.adaptivegateway.auth.dto.LoginRequest;
import com.adaptivegateway.auth.dto.RefreshTokenRequest;
import com.adaptivegateway.auth.dto.RegisterUserRequest;
import com.adaptivegateway.auth.dto.SetupAdminRequest;
import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.entity.LoginAuditLog;
import com.adaptivegateway.auth.entity.RefreshToken;
import com.adaptivegateway.auth.entity.Role;
import com.adaptivegateway.auth.enums.LoginOutcome;
import com.adaptivegateway.auth.enums.UserStatus;
import com.adaptivegateway.auth.mapper.AuthMapper;
import com.adaptivegateway.auth.repository.AppUserRepository;
import com.adaptivegateway.auth.repository.LoginAuditLogRepository;
import com.adaptivegateway.auth.repository.RefreshTokenRepository;
import com.adaptivegateway.auth.repository.RoleRepository;
import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.redis.service.RedisSessionCacheService;
import com.adaptivegateway.security.AuthSecurityProperties;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAuditLogRepository loginAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthMapper authMapper;
    private final AuthSecurityProperties securityProperties;
    private final RedisSessionCacheService redisSessionCacheService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginAuditLogRepository loginAuditLogRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuthMapper authMapper,
            AuthSecurityProperties securityProperties,
            RedisSessionCacheService redisSessionCacheService
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAuditLogRepository = loginAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authMapper = authMapper;
        this.securityProperties = securityProperties;
        this.redisSessionCacheService = redisSessionCacheService;
    }

    @Transactional
    public AuthResponse setupAdmin(SetupAdminRequest request, AuthRequestContext context) {
        if (appUserRepository.countActiveUsers() > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Administrator setup is already closed");
        }

        String configuredAdminRole = normalizeRoleName(securityProperties.adminRoleName());
        String requestedRole = normalizeRoleName(request.roleName());
        if (!configuredAdminRole.equals(requestedRole)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Setup role must match the configured administrator role");
        }

        Role role = roleRepository.findByNameIgnoreCaseAndDeletedAtIsNull(requestedRole)
                .orElseGet(() -> createRole(requestedRole, request.roleDescription()));
        AppUser user = createUser(request.username(), request.email(), request.password(), request.fullName(), role);
        recordAudit(user, user.getUsername(), LoginOutcome.SUCCESS, null, context);
        return issueTokens(user);
    }

    @Transactional
    public CurrentUserResponse registerUser(RegisterUserRequest request) {
        Role role = roleRepository.findByNameIgnoreCaseAndDeletedAtIsNull(normalizeRoleName(request.roleName()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role does not exist"));
        AppUser user = createUser(request.username(), request.email(), request.password(), request.fullName(), role);
        return authMapper.toCurrentUserResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, AuthRequestContext context) {
        AppUser user = appUserRepository.findActiveByUsername(request.usernameOrEmail())
                .or(() -> appUserRepository.findActiveByEmail(request.usernameOrEmail()))
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordAudit(user, request.usernameOrEmail(), LoginOutcome.FAILED, "Invalid credentials", context);
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid username, email, or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            recordAudit(user, request.usernameOrEmail(), LoginOutcome.LOCKED, "User account is not active", context);
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "User account is not active");
        }

        user.setLastLoginAt(Instant.now());
        AppUser savedUser = appUserRepository.save(user);
        recordAudit(savedUser, savedUser.getUsername(), LoginOutcome.SUCCESS, null, context);
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, AuthRequestContext context) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(hashToken(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token is invalid"));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token is expired or revoked");
        }

        AppUser user = appUserRepository.findActiveById(refreshToken.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token user is invalid"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "User account is not active");
        }

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
        recordAudit(user, user.getUsername(), LoginOutcome.TOKEN_REFRESHED, null, context);
        return issueTokens(user);
    }

    @Transactional
    public CurrentUserResponse logout(String userId, RefreshTokenRequest request, AuthRequestContext context) {
        AppUser user = findCurrentUser(userId);
        refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(hashToken(request.refreshToken()))
                .ifPresent(token -> {
                    if (token.getUser().getId().equals(user.getId()) && token.getRevokedAt() == null) {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    }
                });
        recordAudit(user, user.getUsername(), LoginOutcome.LOGOUT, null, context);
        redisSessionCacheService.evictSession(user.getId());
        return authMapper.toCurrentUserResponse(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String userId) {
        return authMapper.toCurrentUserResponse(findCurrentUser(userId));
    }

    private AppUser createUser(String username, String email, String password, String fullName, Role role) {
        if (appUserRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username.trim())) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Username is already registered");
        }
        if (appUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Email is already registered");
        }

        AppUser user = new AppUser();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return appUserRepository.save(user);
    }

    private Role createRole(String roleName, String roleDescription) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(roleDescription == null || roleDescription.isBlank() ? null : roleDescription.trim());
        return roleRepository.save(role);
    }

    private AuthResponse issueTokens(AppUser user) {
        Instant accessTokenExpiresAt = Instant.now().plus(securityProperties.jwt().accessTokenTtl());
        String accessToken = jwtTokenService.createAccessToken(user, accessTokenExpiresAt);
        String refreshTokenValue = generateRefreshToken();
        Instant refreshTokenExpiresAt = Instant.now().plus(securityProperties.jwt().refreshTokenTtl());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(refreshTokenValue));
        refreshToken.setExpiresAt(refreshTokenExpiresAt);
        refreshTokenRepository.save(refreshToken);
        redisSessionCacheService.cacheSession(user, accessTokenExpiresAt, refreshTokenExpiresAt);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                TOKEN_TYPE,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                authMapper.toCurrentUserResponse(user)
        );
    }

    private AppUser findCurrentUser(String userId) {
        UUID id = parseUserId(userId);
        return appUserRepository.findActiveById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authenticated user does not exist"));
    }

    private UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token subject is invalid");
        }
    }

    private void recordAudit(
            AppUser user,
            String usernameAttempted,
            LoginOutcome outcome,
            String failureReason,
            AuthRequestContext context
    ) {
        LoginAuditLog auditLog = new LoginAuditLog();
        auditLog.setUser(user);
        auditLog.setUsernameAttempted(usernameAttempted.trim());
        auditLog.setOutcome(outcome);
        auditLog.setFailureReason(failureReason);
        auditLog.setUserAgent(context.userAgent());
        auditLog.setIpAddress(parseIpAddress(context.sourceIp()));
        loginAuditLogRepository.save(auditLog);
    }

    private InetAddress parseIpAddress(String sourceIp) {
        if (sourceIp == null || sourceIp.isBlank()) {
            return null;
        }
        try {
            return InetAddress.getByName(sourceIp);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private String normalizeRoleName(String roleName) {
        return roleName.trim().toUpperCase(Locale.ROOT);
    }
}
