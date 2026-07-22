# Phase 9: Adaptive Learning Module

## Objective

Implement traffic-aware adaptive learning that uses real gateway request logs to calculate route metrics, maintain traffic heatmap buckets, and adjust adaptive-enabled rate-limit policy strictness.

This phase does not implement anomaly detection, frontend screens, WebSocket updates, analytics dashboards, API consumer credentials, or deployment.

## Added Backend Capabilities

- Aggregation query over durable `request_logs`.
- Route metric persistence into `route_metrics`.
- Traffic heatmap bucket persistence into `traffic_heatmap_buckets`.
- Adaptive strictness changes for `rate_limit_policies.adaptive_enabled = true`.
- Adjustment audit persistence into `rate_limit_adjustments`.
- Kafka outbox event recording for `gateway.adaptive.strictness_adjusted`.
- Scheduled adaptive learning cycle.
- Admin-only REST APIs for running the cycle and reading outputs.

## REST Endpoints

All endpoints require the configured administrator authority:

```text
POST /api/adaptive-learning/run
GET  /api/adaptive-learning/route-metrics
GET  /api/adaptive-learning/traffic-heatmap
GET  /api/adaptive-learning/adjustments
```

## Learning Flow

1. Read real gateway traffic from `request_logs` for the configured metric window.
2. Group traffic by gateway route.
3. Calculate requests per second, blocked rate, error rate, and average latency.
4. Persist a `route_metrics` row per evaluated route.
5. Upsert the current hourly `traffic_heatmap_buckets` row per evaluated route.
6. Load active route-scoped rate-limit assignments.
7. Consider only adaptive-enabled active policies.
8. Tighten policy strictness when error rate reaches the configured threshold.
9. Relax policy strictness when blocked rate is high and error rate remains below the configured safety threshold.
10. Persist each strictness change in `rate_limit_adjustments`.
11. Record a `gateway.adaptive.strictness_adjusted` event through the PostgreSQL-backed Kafka outbox.

## Environment Variables

Phase 9 uses the adaptive learning variables already present in application configuration:

```text
ADAPTIVE_LEARNING_METRIC_WINDOW_SECONDS
ADAPTIVE_LEARNING_AGGREGATION_INTERVAL_MS
ADAPTIVE_LEARNING_MIN_REQUESTS_FOR_ADJUSTMENT
ADAPTIVE_LEARNING_BLOCKED_RATE_RELAX_THRESHOLD
ADAPTIVE_LEARNING_ERROR_RATE_TIGHTEN_THRESHOLD
ADAPTIVE_LEARNING_MAX_ERROR_RATE_FOR_RELAXATION
ADAPTIVE_LEARNING_STRICTNESS_ADJUSTMENT_STEP
ADAPTIVE_LEARNING_MIN_STRICTNESS_FACTOR
ADAPTIVE_LEARNING_MAX_STRICTNESS_FACTOR
```

## Database Connection

The adaptive learning module uses real PostgreSQL tables from Phase 2:

- `request_logs`
- `route_metrics`
- `traffic_heatmap_buckets`
- `rate_limit_assignments`
- `rate_limit_policies`
- `rate_limit_adjustments`
- `kafka_event_outbox`

Redis counters remain the runtime counter mechanism from Phase 8. PostgreSQL remains the source of truth for policies, assignments, metrics, and adjustments.

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- Existing authentication, gateway, Redis, Kafka, and rate-limit tests still pass.
- Static scan for forbidden implementation markers.

Runtime endpoint verification still requires real PostgreSQL, Redis, Kafka, and all required environment variables.
