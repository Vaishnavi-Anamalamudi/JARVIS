package com.adaptivegateway.auth.controller;

import com.adaptivegateway.auth.dto.AuthResponse;
import com.adaptivegateway.auth.dto.CurrentUserResponse;
import com.adaptivegateway.auth.dto.LoginRequest;
import com.adaptivegateway.auth.dto.RefreshTokenRequest;
import com.adaptivegateway.auth.dto.RegisterUserRequest;
import com.adaptivegateway.auth.dto.SetupAdminRequest;
import com.adaptivegateway.auth.service.AuthService;
import com.adaptivegateway.common.api.ApiResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/setup-admin")
    @Operation(summary = "Create the first administrator when the user table is empty")
    public ApiResponse<AuthResponse> setupAdmin(
            @Valid @RequestBody SetupAdminRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        AuthResponse response = authService.setupAdmin(request, requestContext(exchange));
        return ApiResponse.success(correlationId, "Administrator account created", response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and issue JWT tokens")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        AuthResponse response = authService.login(request, requestContext(exchange));
        return ApiResponse.success(correlationId, "Login successful", response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an access token with a durable refresh token")
    public ApiResponse<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        AuthResponse response = authService.refresh(request, requestContext(exchange));
        return ApiResponse.success(correlationId, "Token refreshed", response);
    }

    @PostMapping("/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Register a user with an existing role")
    public ApiResponse<CurrentUserResponse> registerUser(
            @Valid @RequestBody RegisterUserRequest request,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        CurrentUserResponse response = authService.registerUser(request);
        return ApiResponse.success(correlationId, "User registered", response);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke a refresh token")
    public ApiResponse<CurrentUserResponse> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId,
            ServerWebExchange exchange
    ) {
        CurrentUserResponse response = authService.logout(jwt.getSubject(), request, requestContext(exchange));
        return ApiResponse.success(correlationId, "Logout successful", response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Read the authenticated user profile")
    public ApiResponse<CurrentUserResponse> me(
            @AuthenticationPrincipal Jwt jwt,
            @RequestAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE) String correlationId
    ) {
        CurrentUserResponse response = authService.currentUser(jwt.getSubject());
        return ApiResponse.success(correlationId, "Current user read successfully", response);
    }

    private AuthRequestContext requestContext(ServerWebExchange exchange) {
        String sourceIp = exchange.getRequest().getRemoteAddress() == null
                ? null
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        return new AuthRequestContext(sourceIp, userAgent);
    }

    public record AuthRequestContext(String sourceIp, String userAgent) {
    }
}
