# Phase 14: Analytics

## Objective

Implement real analytics rollups and dashboard analytics screens using persisted gateway request history from PostgreSQL.

This phase does not implement deployment packaging, container orchestration, cloud infrastructure, or report export storage.

## Added Backend Capabilities

- Analytics domain package under `backend/src/main/java/com/adaptivegateway/analytics`.
- JPA entities for existing Phase 2 tables:
  - `analytics_rollups`
  - `client_metrics`
- Repository queries for rollup search, client metric search, and existing rollup upserts.
- Native PostgreSQL aggregation from `request_logs`.
- Summary analytics over a configurable recent traffic window.
- Minute, hour, and day rollup generation.
- Global, route-scoped, consumer-scoped, and route-plus-consumer rollups through PostgreSQL grouping sets.
- Per-consumer client metric generation.
- Average latency, p95 latency, blocked rate, error rate, and unique source IP calculations.
- Live analytics completion events through the Phase 13 WebSocket stream.
- Admin-only analytics REST APIs.

## Added Frontend Capabilities

- Dedicated Analytics page in the operations console.
- Summary KPI cards for requests, blocked rate, error rate, and p95 latency.
- Rollup generation controls for minute, hour, and day granularity.
- Client metric generation controls.
- Rollup volume chart with allowed, blocked, and error request segments.
- Client health chart for RPS, blocked rate, and error rate.
- Rollup and client metric tables backed by real REST APIs.
- Live refresh when analytics rollups, client metrics, or request-completed events arrive.

## REST Endpoints

All endpoints require the configured administrator authority:

```text
GET  /api/analytics/summary
POST /api/analytics/rollups/run
GET  /api/analytics/rollups
POST /api/analytics/client-metrics/run
GET  /api/analytics/client-metrics
```

## WebSocket Event Types

Phase 14 adds these live events:

```text
gateway.analytics.rollups_completed
gateway.analytics.client_metrics_completed
```

## Database Connection

Analytics reads and writes real PostgreSQL tables from Phase 2:

- `request_logs`
- `analytics_rollups`
- `client_metrics`
- `api_consumers`
- `gateway_routes`

No mock analytics feed, static JSON fixture, client-side fake counters, or in-memory rollup store is used.

## Rollup Behavior

Rollup generation:

1. Truncates the current time to the selected granularity.
2. Evaluates complete historical buckets only.
3. Aggregates request history from `request_logs`.
4. Writes or updates matching `analytics_rollups`.
5. Emits a live analytics completion event.

Client metric generation:

1. Reads recent `request_logs` for the requested window.
2. Groups traffic by API consumer.
3. Calculates requests per second, blocked rate, error rate, and average latency.
4. Persists `client_metrics`.
5. Emits a live analytics completion event.

## Verification

Verified in this phase:

- Backend compilation.
- Full backend unit test execution.
- Analytics rollup generation writes request-log aggregates.
- Client metric generation calculates per-consumer RPS and rates.
- Frontend TypeScript app code type-checks.
- Vite config type-checks.
- Production frontend build succeeds.
- Frontend npm audit reports zero vulnerabilities.
- Existing Phase 1-13 tests and builds still pass.

Runtime endpoint verification still requires the backend runtime with PostgreSQL, Redis, Kafka, JWT configuration, and an administrator access token.
