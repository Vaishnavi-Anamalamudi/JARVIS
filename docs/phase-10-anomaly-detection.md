# Phase 10: Anomaly Detection

## Objective

Implement statistical anomaly detection over persisted route metrics using rolling mean, rolling variance, rolling standard deviation, rolling z-score, exponential moving average, and adaptive thresholds.

This phase does not implement frontend, WebSocket delivery, full analytics screens, API consumer credential management, or deployment.

## Added Backend Capabilities

- Validated anomaly detection configuration.
- Repositories and services for `anomaly_stat_snapshots`, `anomaly_records`, and `alerts`.
- Scheduled anomaly detection cycle.
- Admin-triggered manual anomaly detection cycle.
- Rolling statistic snapshot creation from persisted `route_metrics`.
- Anomaly record creation when observed metrics exceed z-score and adaptive-threshold rules.
- Alert creation for detected anomalies.
- Adaptive strictness increase for active adaptive policies assigned to anomalous routes.
- Kafka outbox event publication for detected anomalies.
- Admin read APIs for anomaly records, statistic snapshots, and alerts.

## REST Endpoints

All endpoints require the configured administrator authority:

```text
POST /api/anomalies/run
GET  /api/anomalies
GET  /api/anomalies/snapshots
GET  /api/alerts
```

## Detection Inputs

Anomaly detection reads real metrics produced by Phase 9:

- `route_metrics`
- active adaptive route assignments
- adaptive-enabled rate limit policies

No machine learning library, mock metric feed, or in-memory dataset is used.

## Detection Logic

The detection cycle evaluates:

- `blocked_rate`
- `error_rate`

For each route with enough samples, it calculates:

- rolling mean
- rolling variance
- rolling standard deviation
- rolling z-score
- exponential moving average
- adaptive threshold

An anomaly is recorded when the latest observed value exceeds both the configured z-score threshold and adaptive threshold.

## Environment Variables

Phase 10 adds these required variables:

```text
ANOMALY_LOOKBACK_MINUTES
ANOMALY_MINIMUM_SAMPLES
ANOMALY_Z_SCORE_THRESHOLD
ANOMALY_THRESHOLD_STDDEV_MULTIPLIER
ANOMALY_EMA_ALPHA
ANOMALY_DETECTION_INTERVAL_MS
ANOMALY_STRICTNESS_INCREASE_STEP
ANOMALY_MAX_STRICTNESS_FACTOR
```

## Database Connection

The anomaly detection module uses real PostgreSQL tables from Phase 2:

- `route_metrics`
- `anomaly_stat_snapshots`
- `anomaly_records`
- `alerts`
- `rate_limit_assignments`
- `rate_limit_policies`
- `rate_limit_adjustments`
- `kafka_event_outbox`

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- Rolling metric spike creates statistic snapshots.
- Anomaly record creation from threshold breach.
- Alert creation for detected anomaly.
- Existing Phase 1-9 tests still pass.

Runtime verification still requires real PostgreSQL, Redis, Kafka, route metrics, and the required environment variables.
