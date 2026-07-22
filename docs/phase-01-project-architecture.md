# Phase 1: Project Architecture

## Objective

Define the complete production architecture for an Adaptive API Gateway with Traffic-Aware Rate Limiting before generating implementation modules.

This phase does not create runtime code. It defines the architecture contract that all later phases must follow.

## System Overview

The system is a full-stack API gateway platform that authenticates users, registers API consumers, routes API traffic, enforces adaptive rate limits, records traffic, publishes request events, detects anomalies, and streams live dashboard updates.

The production runtime contains these services:

- React TypeScript frontend served through Vercel or NGINX.
- Spring Boot backend using Spring Cloud Gateway.
- PostgreSQL as the system of record.
- Redis for counters, buckets, sessions, and temporary live statistics.
- Apache Kafka for request, analytics, anomaly, alert, and audit event streams.
- WebSocket channel for live dashboard updates.
- NGINX reverse proxy for local Docker Compose and production edge routing.

## Runtime Topology

Client browser

-> Frontend React application

-> Backend REST and WebSocket endpoints

-> Gateway routing and filtering layer

-> Rate limiting and adaptive decision layer

-> Registered upstream APIs

Supporting data flow:

- PostgreSQL stores durable users, roles, clients, routes, request logs, rate-limit policies, alerts, analytics snapshots, reports, and audit logs.
- Redis stores sliding-window counters, token-bucket state, session cache, current traffic statistics, and short-lived dashboard aggregates.
- Kafka receives immutable domain events for every request and every important system action.
- Kafka consumers update analytics, anomaly state, audit records, alerts, and WebSocket dashboard messages.

## Backend Architecture

Backend root in later phases:

```text
backend/
  src/main/java/com/adaptivegateway/
    AdaptiveGatewayApplication.java
    config/
    security/
    common/
      api/
      dto/
      exception/
      pagination/
      validation/
    auth/
    users/
    clients/
    gateway/
    ratelimit/
    adaptive/
    anomaly/
    analytics/
    alerts/
    audit/
    websocket/
    kafka/
    redis/
  src/main/resources/
    application.yml
    application-local.yml
    application-prod.yml
    db/migration/
```

Each backend feature module must use this structure where relevant:

```text
module/
  controller/
  dto/
  entity/
  enums/
  event/
  mapper/
  repository/
  service/
  specification/
```

Required backend rules:

- Java 21 and Spring Boot.
- Maven wrapper in the backend module.
- PostgreSQL only.
- UUID primary keys for database entities.
- DTOs for every request and response.
- Repository pattern through Spring Data JPA.
- Service layer for business logic.
- Bean validation on request DTOs.
- Global exception handling with stable error codes.
- Structured logging for security, gateway, rate-limit, and event operations.
- OpenAPI documentation for every endpoint.
- Pagination, sorting, and filtering for list endpoints.
- No embedded database profile.

## Frontend Architecture

Frontend root in later phases:

```text
frontend/
  src/
    app/
      store.ts
      router.tsx
    api/
      axiosClient.ts
    auth/
    features/
      dashboard/
      clients/
      gateway/
      ratelimits/
      analytics/
      alerts/
      audit/
      reports/
    components/
      layout/
      feedback/
      data-display/
      forms/
    hooks/
    theme/
    types/
    utils/
```

Required frontend rules:

- React with TypeScript and Vite.
- TailwindCSS for layout utilities.
- Material UI for accessible, production-grade controls.
- Redux Toolkit for application state.
- Axios for authenticated REST calls.
- React Router with protected routes.
- Recharts for analytics visualizations.
- Socket.IO Client for live dashboard updates.
- No static runtime data in screens.
- Loading states, empty states from real API responses, error states, and toast notifications.
- Responsive layout and dark mode.

## Database Architecture

Database design is implemented in Phase 2, but all later tables must follow these rules:

- PostgreSQL only.
- Flyway migrations.
- UUID primary keys.
- Normalized tables with foreign keys.
- Indexes for lookup, filtering, time-series access, and unique business constraints.
- Audit columns on durable tables:
  - `created_at`
  - `updated_at`
  - `deleted_at`
- Soft delete where records are user-managed or operationally sensitive.
- No duplicate tables for the same concept.

Planned durable domains:

- Identity and access: users, roles, refresh tokens, login audit.
- Consumer management: API consumers, credentials, ownership, status.
- Gateway configuration: upstream services, gateway routes, route predicates.
- Rate limiting: policies, assignments, decisions, adjustments.
- Traffic: request logs and request outcomes.
- Analytics: rollups, client metrics, route metrics, heatmap buckets.
- Anomaly detection: rolling statistics, anomaly records, strictness changes.
- Alerts: alert records, notification state, acknowledgement state.
- Audit: immutable security and admin action logs.
- Reports: export records and report metadata.

## Event Architecture

Kafka is implemented in Phase 7. The architecture reserves these event categories:

- `gateway.request.received`
- `gateway.request.completed`
- `gateway.request.blocked`
- `rate_limit.decision.recorded`
- `rate_limit.policy.adjusted`
- `analytics.metric.calculated`
- `anomaly.detected`
- `alert.created`
- `audit.recorded`

Each Kafka event must include:

- Event id.
- Event type.
- Occurred timestamp.
- Correlation id.
- Actor id when available.
- Client id when available.
- Route id when available.
- Source service.
- Schema version.
- Event payload.

## Redis Architecture

Redis is implemented in Phase 6. The architecture reserves Redis for short-lived operational state:

- Sliding-window request counters.
- Token-bucket token counters and refill timestamps.
- Authenticated session cache.
- Temporary traffic statistics.
- Current dashboard aggregates.
- Rate-limit strictness cache.

Redis key names must be versioned and namespaced by domain:

```text
agw:v1:{domain}:{entity}:{id}:{metric}
```

Durable records must remain in PostgreSQL. Redis is never the source of truth for business entities.

## WebSocket Architecture

WebSocket is implemented in Phase 13 after REST APIs and frontend foundations exist.

Live update channels:

- Traffic overview.
- Requests per second.
- Blocked requests.
- Top clients.
- Current rate limits.
- Alerts.
- Dashboard charts.

WebSocket messages must be derived from real Kafka consumers, database records, or Redis statistics. The dashboard must not invent client-side values.

## Security Architecture

Authentication and authorization are implemented in Phase 4.

Security requirements:

- JWT access tokens.
- Refresh-token persistence in PostgreSQL.
- Password hashing with a production password encoder.
- Role-based access control.
- Admin and user dashboards separated by authorization checks.
- Protected REST routes.
- Protected frontend routes.
- Security audit logs for authentication, authorization failures, admin changes, and suspicious activity.
- CORS and security headers configured per environment.

## Adaptive Rate Limiting Architecture

Rate limiting is implemented across Phases 8, 9, and 10.

Core algorithms:

- Sliding window for precise request counting over time.
- Token bucket for burst handling.
- Dynamic limit adjustment using real traffic statistics.
- Rolling mean.
- Rolling variance.
- Rolling standard deviation.
- Rolling Z score.
- Exponential moving average.
- Adaptive thresholding.

Decision flow:

1. Gateway receives request.
2. Authentication and client identity are resolved.
3. Gateway route is resolved.
4. Active rate-limit policy is loaded from PostgreSQL and cached where appropriate.
5. Redis counters are updated atomically.
6. Rate-limit decision is produced.
7. Request is allowed or blocked.
8. Kafka event is published.
9. PostgreSQL request log is written.
10. Analytics and anomaly consumers process the event.
11. Dashboard receives live update through WebSocket.

## Deployment Architecture

Deployment is implemented in Phase 15.

Local production-like runtime:

- Docker Compose.
- Backend container.
- Frontend container.
- PostgreSQL container.
- Redis container.
- Kafka container.
- NGINX container.

Public deployment targets:

- Frontend: Vercel.
- Backend: Render.
- Database: Neon PostgreSQL.
- Redis: Redis Cloud.
- CI/CD: GitHub Actions.

Environment configuration must be separated:

- Local development.
- Docker Compose.
- Production.
- CI.

Secrets must be injected through environment variables and never committed.

## Phase Verification Gates

Every implementation phase after Phase 1 must verify:

- The project compiles.
- The backend starts when backend code exists.
- The frontend starts when frontend code exists.
- PostgreSQL schema changes apply when migrations exist.
- Redis behavior is verified when Redis integration exists.
- Kafka publish and consume paths are verified when Kafka integration exists.
- WebSocket live updates are verified when WebSocket integration exists.
- REST APIs are verified with real persistence.
- Frontend screens render real API data.

## Phase 1 Completion Criteria

Phase 1 is complete when:

- System topology is documented.
- Backend module architecture is documented.
- Frontend module architecture is documented.
- Database rules are documented.
- Redis responsibilities are documented.
- Kafka event architecture is documented.
- WebSocket channels are documented.
- Security architecture is documented.
- Rate-limit and anomaly architecture is documented.
- Deployment architecture is documented.
- Later-phase verification gates are documented.

All criteria are satisfied in this file.
