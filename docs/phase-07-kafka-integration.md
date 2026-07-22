# Phase 7: Kafka Integration

## Objective

Integrate Apache Kafka into the backend runtime using a PostgreSQL-backed outbox so request events can be durably recorded and published without making Kafka the source of truth.

This phase does not implement rate limiting decisions, adaptive learning, anomaly detection, analytics consumers, frontend, WebSocket, request-log persistence, or deployment.

## Added Backend Capabilities

- Spring Kafka producer support.
- Environment-driven Kafka producer configuration.
- Validated Kafka integration properties.
- JPA entity and repository for `kafka_event_outbox`.
- Transactional Kafka outbox event recording.
- Synchronous publish attempt after outbox persistence.
- Retryable scheduled publishing for due pending outbox events.
- Admin-only REST APIs for Kafka outbox inspection and pending-event publishing.
- Request-completed Kafka event publishing for backend HTTP requests.
- Real Kafka health verification through a producer send to the health topic.
- Structured `KAFKA_UNAVAILABLE` error code.
- Kafka details included in `GET /api/system/health`.

## REST Endpoints

All Kafka management endpoints require the configured administrator authority:

```text
GET  /api/kafka/outbox
GET  /api/kafka/outbox/{id}
POST /api/kafka/outbox/publish-pending
```

`GET /api/system/health` remains public and now checks backend, PostgreSQL, Redis, and Kafka.

## Kafka Topics

Kafka topics are configured through environment variables rather than hardcoded runtime values:

```text
KAFKA_DEFAULT_TOPIC
KAFKA_REQUEST_EVENTS_TOPIC
KAFKA_HEALTH_TOPIC
```

Current event types:

```text
gateway.request.completed
system.kafka.health
```

`gateway.request.completed` events include request method, path, status code, outcome, duration, source IP, and user agent.

## Outbox Behavior

Every backend HTTP request records a `gateway.request.completed` event through the outbox service after the response completes.

The outbox publisher:

- saves the event to `kafka_event_outbox`
- attempts to publish it to Kafka
- marks it `PUBLISHED` when Kafka acknowledges the send
- keeps it `PENDING` with `next_attempt_at` when the send fails before the max-attempt threshold
- marks it `FAILED` after configured attempts are exhausted

The scheduled publisher retries due `PENDING` events using:

```text
KAFKA_OUTBOX_POLL_INTERVAL_MS
KAFKA_OUTBOX_BATCH_SIZE
KAFKA_RETRY_DELAY_SECONDS
KAFKA_MAX_ATTEMPTS
```

## Environment Variables

Phase 7 adds these required variables:

```text
KAFKA_BOOTSTRAP_SERVERS
KAFKA_PRODUCER_RETRIES
KAFKA_DEFAULT_TOPIC
KAFKA_REQUEST_EVENTS_TOPIC
KAFKA_HEALTH_TOPIC
KAFKA_EVENT_SCHEMA_VERSION
KAFKA_PUBLISH_TIMEOUT_MS
KAFKA_RETRY_DELAY_SECONDS
KAFKA_MAX_ATTEMPTS
KAFKA_OUTBOX_BATCH_SIZE
KAFKA_OUTBOX_POLL_INTERVAL_MS
```

## Database Connection

Kafka integration uses the existing PostgreSQL table from Phase 2:

- `kafka_event_outbox`

There is no in-memory event queue and no embedded Kafka fallback.

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- Kafka event outbox persistence and publish status transitions.
- Retry publishing uses the request-events topic for request-completed events.
- Kafka health maps broker failures to `KAFKA_UNAVAILABLE`.

Runtime Kafka verification still requires a real Kafka broker plus the required environment variables. No embedded Kafka or mock runtime broker was added.
