# API Documentation

The backend exposes JSON REST APIs under `/api` and wraps successful responses in:

```json
{
  "success": true,
  "correlationId": "uuid",
  "message": "Operation completed",
  "data": {}
}
```

Errors use the same correlation-id convention and an application error code.

## Authentication

- `POST /api/auth/setup-admin`: create the first administrator while the user table is empty.
- `POST /api/auth/login`: authenticate and issue access/refresh tokens.
- `POST /api/auth/refresh`: rotate a refresh token and issue a new access token.
- `POST /api/auth/logout`: revoke a refresh token.
- `GET /api/auth/me`: read current user.
- `POST /api/auth/users`: register a user with an existing role.

## Gateway Management

- `GET /api/gateway/upstreams`
- `POST /api/gateway/upstreams`
- `GET /api/gateway/upstreams/{id}`
- `PUT /api/gateway/upstreams/{id}`
- `DELETE /api/gateway/upstreams/{id}`
- `GET /api/gateway/routes`
- `POST /api/gateway/routes`
- `GET /api/gateway/routes/{id}`
- `PUT /api/gateway/routes/{id}`
- `DELETE /api/gateway/routes/{id}`
- `POST /api/gateway/routes/refresh`

## Rate Limits

- `GET /api/rate-limit/policies`
- `POST /api/rate-limit/policies`
- `GET /api/rate-limit/policies/{id}`
- `PUT /api/rate-limit/policies/{id}`
- `DELETE /api/rate-limit/policies/{id}`
- `GET /api/rate-limit/assignments`
- `POST /api/rate-limit/assignments`
- `GET /api/rate-limit/assignments/{id}`
- `PUT /api/rate-limit/assignments/{id}`
- `DELETE /api/rate-limit/assignments/{id}`

## Consumers

- `GET /api/consumers`
- `POST /api/consumers`
- `GET /api/consumers/{id}`
- `PUT /api/consumers/{id}`
- `DELETE /api/consumers/{id}`
- `GET /api/consumers/{consumerId}/credentials`
- `POST /api/consumers/{consumerId}/credentials`
- `POST /api/consumers/credentials/{credentialId}/revoke`

## Analytics And Intelligence

- `GET /api/analytics/summary`
- `POST /api/analytics/rollups/run`
- `GET /api/analytics/rollups`
- `POST /api/analytics/client-metrics/run`
- `GET /api/analytics/client-metrics`
- `POST /api/adaptive-learning/run`
- `GET /api/adaptive-learning/route-metrics`
- `GET /api/adaptive-learning/traffic-heatmap`
- `GET /api/adaptive-learning/adjustments`
- `POST /api/anomalies/run`
- `GET /api/anomalies`
- `GET /api/anomalies/snapshots`

## Alerts And Operations

- `GET /api/alerts`
- `POST /api/alerts/{id}/acknowledge`
- `POST /api/alerts/{id}/resolve`
- `GET /api/operations/requests`
- `GET /api/operations/audit-logs`
- `GET /api/system/health`
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Live Stream

`/api/live/ws` is a JWT-protected WebSocket endpoint. Browser clients pass the access token through the `access_token` query parameter and receive live JSON events after real backend persistence.

OpenAPI UI is available at `/swagger-ui.html` when the backend is running.
