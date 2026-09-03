package com.adaptivegateway.alerts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adaptivegateway.alerts.dto.AlertResponse;
import com.adaptivegateway.alerts.entity.Alert;
import com.adaptivegateway.alerts.enums.AlertSeverity;
import com.adaptivegateway.alerts.enums.AlertStatus;
import com.adaptivegateway.alerts.mapper.AlertMapper;
import com.adaptivegateway.alerts.repository.AlertRepository;
import com.adaptivegateway.auth.entity.AppUser;
import com.adaptivegateway.auth.repository.AppUserRepository;
import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.live.service.LiveEventService;
import com.adaptivegateway.operations.service.AuditLogService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AlertQueryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AlertMapper mapper;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private LiveEventService liveEventService;

    private AlertQueryService service;

    @BeforeEach
    void setUp() {
        service = new AlertQueryService(alertRepository, appUserRepository, mapper, auditLogService, liveEventService);
    }

    @Test
    void acknowledgeMovesOpenAlertToAcknowledgedAndRecordsOperationsEvent() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        AppUser actor = actor(userId);
        Alert alert = alert(alertId, AlertStatus.OPEN);

        when(alertRepository.findByIdAndDeletedAtIsNull(alertId)).thenReturn(Optional.of(alert));
        when(appUserRepository.findActiveById(userId)).thenReturn(Optional.of(actor));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Alert.class))).thenReturn(response(alertId, AlertStatus.ACKNOWLEDGED));

        AlertResponse response = service.acknowledge(alertId, jwt(userId), exchange());

        assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.getAcknowledgedByUser()).isSameAs(actor);
        assertThat(alert.getAcknowledgedAt()).isNotNull();
        verify(auditLogService).record(eq(userId.toString()), eq("ALERT_ACKNOWLEDGED"), eq("alert"), eq(alertId), any(), any(Map.class));
        verify(liveEventService).emit(eq("gateway.alert.acknowledged"), eq("alert"), eq(alertId), any(), eq("Alert acknowledged"), any(Map.class));
    }

    @Test
    void resolveSetsResolvedTimestampAndActorWhenOpen() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        Alert alert = alert(alertId, AlertStatus.OPEN);

        when(alertRepository.findByIdAndDeletedAtIsNull(alertId)).thenReturn(Optional.of(alert));
        when(appUserRepository.findActiveById(userId)).thenReturn(Optional.of(actor(userId)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Alert.class))).thenReturn(response(alertId, AlertStatus.RESOLVED));

        AlertResponse response = service.resolve(alertId, jwt(userId), exchange());

        assertThat(response.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getAcknowledgedByUser()).isNotNull();
        assertThat(alert.getAcknowledgedAt()).isNotNull();
        assertThat(alert.getResolvedAt()).isNotNull();

        ArgumentCaptor<Map<String, Object>> metadata = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(userId.toString()), eq("ALERT_RESOLVED"), eq("alert"), eq(alertId), any(), metadata.capture());
        assertThat(metadata.getValue()).containsEntry("status", "RESOLVED");
        verify(liveEventService).emit(eq("gateway.alert.resolved"), eq("alert"), eq(alertId), any(), eq("Alert resolved"), any(Map.class));
    }

    @Test
    void acknowledgeRejectsResolvedAlert() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findByIdAndDeletedAtIsNull(alertId)).thenReturn(Optional.of(alert(alertId, AlertStatus.RESOLVED)));

        assertThatThrownBy(() -> service.acknowledge(alertId, jwt(UUID.randomUUID()), exchange()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Resolved alerts cannot be acknowledged");
    }

    private Alert alert(UUID id, AlertStatus status) {
        Alert alert = new Alert();
        ReflectionTestUtils.setField(alert, "id", id);
        alert.setAlertType("TRAFFIC_SPIKE");
        alert.setSeverity(AlertSeverity.HIGH);
        alert.setTitle("Traffic spike");
        alert.setMessage("Route traffic exceeded the learned threshold");
        alert.setStatus(status);
        return alert;
    }

    private AppUser actor(UUID id) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("admin");
        return user;
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("username", "admin")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/api/alerts/action").build());
    }

    private AlertResponse response(UUID id, AlertStatus status) {
        return new AlertResponse(
                id,
                null,
                null,
                null,
                "TRAFFIC_SPIKE",
                AlertSeverity.HIGH,
                "Traffic spike",
                "Route traffic exceeded the learned threshold",
                status,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
