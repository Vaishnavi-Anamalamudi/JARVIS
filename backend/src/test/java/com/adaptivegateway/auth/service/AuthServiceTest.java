package com.adaptivegateway.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.auth.controller.AuthController.AuthRequestContext;
import com.adaptivegateway.auth.dto.LoginRequest;
import com.adaptivegateway.auth.dto.RefreshTokenRequest;
import com.adaptivegateway.auth.dto.SetupAdminRequest;
import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.entity.LoginAuditLog;
import com.adaptivegateway.auth.entity.RefreshToken;
import com.adaptivegateway.auth.entity.Role;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final AuthRequestContext REQUEST_CONTEXT = new AuthRequestContext("127.0.0.1", "JUnit");

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginAuditLogRepository loginAuditLogRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RedisSessionCacheService redisSessionCacheService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        AuthSecurityProperties properties = new AuthSecurityProperties(
                "ADMIN",
                new AuthSecurityProperties.Jwt("adaptive-gateway-test", jwtSecret(), 15, 7)
        );
        authService = new AuthService(
                appUserRepository,
                roleRepository,
                refreshTokenRepository,
                loginAuditLogRepository,
                passwordEncoder,
                jwtTokenService,
                new AuthMapper(),
                properties,
                redisSessionCacheService
        );
    }

    @Test
    void setupAdminCreatesAdminRoleUserAuditAndTokensWhenNoActiveUsersExist() {
        when(appUserRepository.countActiveUsers()).thenReturn(0L);
        when(roleRepository.findByNameIgnoreCaseAndDeletedAtIsNull("ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(appUserRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull("admin")).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("Admin@Example.com")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(jwtTokenService.createAccessToken(any(AppUser.class), any(Instant.class))).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAuditLogRepository.save(any(LoginAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.setupAdmin(new SetupAdminRequest(
                "admin",
                "Admin@Example.com",
                "very-strong-password",
                "Admin User",
                "admin",
                "Gateway administrators"
        ), REQUEST_CONTEXT);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().email()).isEqualTo("admin@example.com");
        assertThat(response.user().role()).isEqualTo("ADMIN");

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertThat(passwordEncoder.matches("very-strong-password", userCaptor.getValue().getPasswordHash())).isTrue();

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getName()).isEqualTo("ADMIN");

        verify(loginAuditLogRepository).save(any(LoginAuditLog.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(redisSessionCacheService).cacheSession(any(AppUser.class), any(Instant.class), any(Instant.class));
    }

    @Test
    void setupAdminRejectsRequestsAfterActiveUsersExist() {
        when(appUserRepository.countActiveUsers()).thenReturn(1L);

        assertThatThrownBy(() -> authService.setupAdmin(new SetupAdminRequest(
                "admin",
                "admin@example.com",
                "very-strong-password",
                "Admin User",
                "ADMIN",
                null
        ), REQUEST_CONTEXT))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);

        verify(roleRepository, never()).save(any(Role.class));
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void loginRecordsFailedAuditForInvalidCredentials() {
        AppUser user = activeUser("operator", "operator@example.com", "correct-password", "OPERATOR");
        when(appUserRepository.findActiveByUsername("operator")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("operator", "wrong-password"),
                REQUEST_CONTEXT
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTHENTICATION_FAILED);

        verify(loginAuditLogRepository).save(any(LoginAuditLog.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(redisSessionCacheService, never()).cacheSession(any(AppUser.class), any(Instant.class), any(Instant.class));
    }

    @Test
    void refreshRevokesCurrentTokenAndIssuesReplacement() throws Exception {
        AppUser user = activeUser("operator", "operator@example.com", "correct-password", "OPERATOR");
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(sha256("refresh-token-value"));
        storedToken.setExpiresAt(Instant.now().plusSeconds(300));

        when(refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(sha256("refresh-token-value")))
                .thenReturn(Optional.of(storedToken));
        when(appUserRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenService.createAccessToken(any(AppUser.class), any(Instant.class))).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAuditLogRepository.save(any(LoginAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.refresh(new RefreshTokenRequest("refresh-token-value"), REQUEST_CONTEXT);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isNotEqualTo("refresh-token-value");
        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(redisSessionCacheService).cacheSession(any(AppUser.class), any(Instant.class), any(Instant.class));

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getAllValues().get(1).getTokenHash()).isNotEqualTo(sha256("refresh-token-value"));
    }

    private AppUser activeUser(String username, String email, String rawPassword, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        withId(role);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName("Test User");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        withId(user);
        return user;
    }

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    private static String sha256(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private static String jwtSecret() {
        byte[] secret = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(secret);
    }
}
