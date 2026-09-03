package com.adaptivegateway.live.web;

import com.adaptivegateway.live.dto.LiveEventResponse;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.security.AuthSecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LiveWebSocketHandler implements WebSocketHandler {

    private final LiveEventService liveEventService;
    private final ReactiveJwtDecoder jwtDecoder;
    private final AuthSecurityProperties authProperties;
    private final ObjectMapper objectMapper;

    public LiveWebSocketHandler(
            LiveEventService liveEventService,
            ReactiveJwtDecoder jwtDecoder,
            AuthSecurityProperties authProperties,
            ObjectMapper objectMapper
    ) {
        this.liveEventService = liveEventService;
        this.jwtDecoder = jwtDecoder;
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String accessToken = accessToken(session);
        if (accessToken == null || accessToken.isBlank()) {
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        Flux<WebSocketMessage> messages = authorize(accessToken)
                .thenMany(liveEventService.stream())
                .map(event -> toMessage(session, event));

        return session.send(messages)
                .onErrorResume(ignored -> session.close(CloseStatus.POLICY_VIOLATION));
    }

    private Mono<Jwt> authorize(String accessToken) {
        return jwtDecoder.decode(accessToken)
                .filter(this::hasAdminRole)
                .switchIfEmpty(Mono.error(new IllegalStateException("WebSocket access denied")));
    }

    private boolean hasAdminRole(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role == null || role.isBlank()) {
            return false;
        }
        return authority(role).equals(authority(authProperties.adminRoleName()));
    }

    private String accessToken(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst("access_token");
    }

    private WebSocketMessage toMessage(WebSocketSession session, LiveEventResponse event) {
        try {
            return session.textMessage(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Live event serialization failed", exception);
        }
    }

    private String authority(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
