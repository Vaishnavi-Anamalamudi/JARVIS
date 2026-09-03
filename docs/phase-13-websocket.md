# Phase 13: WebSocket

## Objective

Add live dashboard updates after the REST-backed frontend exists, using real backend events emitted from persisted gateway, rate-limit, audit, adaptive-learning, and anomaly workflows.

This phase does not implement dedicated analytics rollups, export workflows, or deployment packaging.

## Added Backend Capabilities

- WebSocket endpoint at `/api/live/ws`.
- JWT validation for WebSocket clients using the same token issuer and secret as REST authentication.
- Administrator-role authorization for live stream access.
- Typed live event envelope through `LiveEventResponse`.
- Multicast live event service with periodic heartbeat events.
- Live events emitted after real saved backend state changes.
- WebSocket route registered through reactive WebFlux handler mapping.

## Added Frontend Capabilities

- Native browser WebSocket client.
- Token-authenticated live connection using the stored access token.
- Automatic reconnect after connection loss.
- Live connection status indicator in the operations shell.
- Vite dev-server WebSocket proxy support for `/api/live/ws`.
- Event-aware refresh behavior for dashboard, gateway, rate-limit, consumers, operations, adaptive, anomaly, and alert panels.

## WebSocket Endpoint

```text
GET /api/live/ws?access_token=<jwt>
```

The browser sends the JWT as a query parameter because standard browser WebSocket construction does not allow custom `Authorization` headers.

## Event Types

The backend emits these live event types:

```text
gateway.live.heartbeat
gateway.request.completed
gateway.audit.logged
gateway.config.changed
gateway.rate_limit.changed
gateway.adaptive.run_completed
gateway.adaptive.strictness_adjusted
gateway.anomaly.run_completed
gateway.anomaly.detected
gateway.analytics.rollups_completed
gateway.analytics.client_metrics_completed
```

## Event Envelope

Every message uses this JSON shape:

```json
{
  "id": "uuid",
  "type": "gateway.request.completed",
  "resourceType": "request_log",
  "resourceId": "uuid",
  "correlationId": "uuid",
  "message": "Gateway request completed",
  "payload": {},
  "createdAt": "2026-08-25T14:45:00Z"
}
```

## Real Data Sources

Live events are emitted from real backend workflows after persistence:

- gateway route and upstream changes
- rate-limit policy and assignment changes
- request-log completion persistence
- audit-log persistence
- adaptive-learning run and strictness adjustment persistence
- anomaly record and alert persistence

No mock stream, client-generated values, or static event feed is used.

## Environment Variables

Phase 13 uses the existing authentication variables:

```text
JWT_ISSUER
JWT_SECRET_BASE64
ADAPTIVE_GATEWAY_ADMIN_ROLE_NAME
```

The frontend can also set:

```text
VITE_API_BASE_URL
VITE_BACKEND_PROXY_TARGET
```

## Verification

Verified in this phase:

- Backend compilation.
- Full backend unit test execution.
- Live event service publishes emitted events to subscribers.
- Frontend TypeScript app code type-checks.
- Vite config type-checks.
- Production frontend build succeeds.
- Frontend npm audit reports zero vulnerabilities.
- Existing Phase 1-12 tests and builds still pass.

Runtime WebSocket handshake verification still requires the backend runtime with valid PostgreSQL, Redis, Kafka, JWT configuration, and an administrator access token.
