# Phase 2: Database Design

## Objective

Define and implement the PostgreSQL system-of-record schema for the Adaptive API Gateway.

The executable database artifact for this phase is:

```text
backend/src/main/resources/db/migration/V1__initial_database_schema.sql
```

This phase does not create backend application code, repositories, services, REST APIs, frontend screens, Redis runtime code, Kafka producers, or WebSocket handlers. Those are added in later phases according to the required order.

## Database Engine

PostgreSQL is the only supported database.

The schema uses:

- Flyway-compatible versioned migration.
- UUID primary keys with `gen_random_uuid()`.
- `pgcrypto` for UUID generation.
- Foreign keys for cross-domain relationships.
- Partial unique indexes for soft-deleted business records.
- Query indexes for authentication, filtering, time-series analytics, gateway routing, alerts, and audit lookups.
- `created_at`, `updated_at`, and `deleted_at` on every durable table.
- A shared `set_updated_at()` trigger function to maintain `updated_at`.

No seed rows, mock rows, sample data, in-memory storage, H2 configuration, or embedded database behavior is introduced in this phase.

## Domain Tables

### Identity And Access

- `roles`: Role definitions for role-based access control.
- `app_users`: Authenticated platform users with email, username, password hash, role, and account status.
- `refresh_tokens`: Durable hashed refresh tokens for JWT renewal and revocation.
- `login_audit_logs`: Authentication attempts, token refresh events, logout events, and security outcomes.

### Consumer Management

- `api_consumers`: Registered API clients owned by users.
- `api_consumer_credentials`: Hashed API credentials, key prefixes, expiry, revocation, and last-use tracking.

### Gateway Configuration

- `upstream_services`: Registered upstream API services with base URLs and operational settings.
- `gateway_routes`: Route definitions connected to upstream services.
- `gateway_route_predicates`: Structured route matching rules stored as validated JSON objects.

### Rate Limiting

- `rate_limit_policies`: Sliding-window, token-bucket, and hybrid policy definitions.
- `rate_limit_assignments`: Policy assignments scoped to consumers, routes, or both.
- `rate_limit_decisions`: Durable record of allow or block decisions for gateway requests.
- `rate_limit_adjustments`: Adaptive strictness changes caused by anomaly or traffic signals.

### Traffic Logging

- `request_logs`: Gateway request records with correlation id, consumer, route, upstream service, outcome, timing, byte counts, status code, and error code.

### Analytics

- `analytics_rollups`: Minute, hour, and day aggregates for dashboard and reports.
- `client_metrics`: Per-consumer calculated traffic metrics.
- `route_metrics`: Per-route calculated traffic metrics.
- `traffic_heatmap_buckets`: Heatmap buckets for day-of-week and hour-of-day traffic visualization.

### Anomaly Detection

- `anomaly_stat_snapshots`: Rolling mean, variance, standard deviation, Z score, EMA, and adaptive threshold snapshots.
- `anomaly_records`: Detected anomalies with severity, metric, observed value, threshold, Z score, status, and description.

### Alerts

- `alerts`: User-visible alert records tied to anomalies, consumers, routes, acknowledgement, and resolution state.

### Audit And Reports

- `audit_logs`: Immutable-style audit entries for security and administrative actions.
- `report_exports`: Export lifecycle metadata for generated reports.

### Event Publishing

- `kafka_event_outbox`: Durable outbox table for reliable Kafka publishing once Kafka integration is implemented in Phase 7.

## Referential Design

Important relationships:

- `app_users.role_id` -> `roles.id`
- `refresh_tokens.user_id` -> `app_users.id`
- `api_consumers.owner_user_id` -> `app_users.id`
- `api_consumer_credentials.consumer_id` -> `api_consumers.id`
- `gateway_routes.upstream_service_id` -> `upstream_services.id`
- `gateway_route_predicates.route_id` -> `gateway_routes.id`
- `rate_limit_assignments.policy_id` -> `rate_limit_policies.id`
- `rate_limit_assignments.consumer_id` -> `api_consumers.id`
- `rate_limit_assignments.route_id` -> `gateway_routes.id`
- `request_logs.consumer_id` -> `api_consumers.id`
- `request_logs.route_id` -> `gateway_routes.id`
- `request_logs.upstream_service_id` -> `upstream_services.id`
- `rate_limit_decisions.request_log_id` -> `request_logs.id`
- `rate_limit_decisions.policy_id` -> `rate_limit_policies.id`
- `anomaly_records.stats_snapshot_id` -> `anomaly_stat_snapshots.id`
- `rate_limit_adjustments.anomaly_record_id` -> `anomaly_records.id`
- `alerts.anomaly_record_id` -> `anomaly_records.id`
- `alerts.acknowledged_by_user_id` -> `app_users.id`
- `audit_logs.actor_user_id` -> `app_users.id`
- `report_exports.requested_by_user_id` -> `app_users.id`

## Constraint Strategy

The schema enforces core business rules at the database level:

- User statuses are constrained to known account states.
- Consumer statuses and environments are constrained.
- Upstream URLs must use HTTP or HTTPS.
- Route paths must begin with `/`.
- Route allowed methods must be non-empty.
- Rate-limit policies must use valid algorithms.
- Token-bucket fields are required only for token-bucket and hybrid policies.
- Assignment scope must include at least one of consumer or route.
- Request status codes must be valid HTTP status codes.
- Analytics rollup counts must reconcile to total requests.
- Rate, latency, byte, and count values must be non-negative.
- Alert acknowledgement and resolution timestamps must match alert status.
- Kafka outbox published rows must include `published_at`.

## Index Strategy

The migration defines indexes for:

- Unique active usernames, emails, roles, credentials, route keys, policy names, and upstream names.
- Authentication token lookup.
- User role and status filtering.
- Consumer owner and status filtering.
- Gateway route lookup by upstream, status, and priority.
- Request history by consumer, route, status, outcome, and time.
- Rate-limit decision history.
- Analytics rollups by bucket, consumer, and route.
- Heatmap buckets by time.
- Anomaly and alert dashboards by status, severity, consumer, route, and time.
- Audit lookup by actor, resource, and correlation id.
- Kafka outbox polling by status and next attempt.

## Soft Delete Strategy

Every durable table includes `deleted_at`.

Business uniqueness uses partial indexes with `WHERE deleted_at IS NULL`, so historical soft-deleted records can remain for auditability while active records retain uniqueness.

Operational event tables also include `deleted_at` for retention management and archival workflows.

## Phase 2 Verification

Verified by static inspection in this phase:

- A Flyway migration exists.
- The migration uses PostgreSQL syntax and `pgcrypto`.
- No seed rows, generated fixture rows, non-production service data, or process-local persistence is present.
- Tables use UUID primary keys.
- Durable tables include audit columns.
- Foreign keys connect related domains.
- Indexes cover authentication, gateway, request history, analytics, anomaly, alert, audit, and outbox access paths.

Runtime database migration execution will be verified after Phase 3 creates the backend foundation and Maven/Flyway runtime.
