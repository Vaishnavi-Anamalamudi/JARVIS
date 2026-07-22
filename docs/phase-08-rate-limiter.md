# Phase 8: Rate Limiter

## Objective

Implement real gateway rate limiting using PostgreSQL configuration, Redis counters, durable request/decision records, and Kafka decision events.

This phase does not implement adaptive learning, anomaly detection, API consumer credential management, frontend, WebSocket, analytics rollups, or deployment.

## Added Backend Capabilities

- JPA entities for `rate_limit_policies`, `rate_limit_assignments`, `request_logs`, and `rate_limit_decisions`.
- Repositories for policy, assignment, request-log, and decision persistence.
- Admin-only REST APIs for rate limit policy management.
- Admin-only REST APIs for route-scoped rate limit assignment management.
- Dynamic gateway enforcement through a Spring Cloud Gateway `GlobalFilter`.
- Redis-backed `SLIDING_WINDOW`, `TOKEN_BUCKET`, and `HYBRID` algorithms.
- Strictness-adjusted effective limits.
- Route/source-IP limiter scope for this phase.
- Structured `429` responses with `RATE_LIMIT_EXCEEDED`.
- `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `Retry-After` headers.
- Durable request log records for routed gateway requests.
- Durable rate limit decision records when a policy assignment applies.
- Kafka outbox event publication for rate limit decisions.

## REST Endpoints

All endpoints require the configured administrator authority:

```text
GET    /api/rate-limit/policies
GET    /api/rate-limit/policies/{id}
POST   /api/rate-limit/policies
PUT    /api/rate-limit/policies/{id}
DELETE /api/rate-limit/policies/{id}

GET    /api/rate-limit/assignments
GET    /api/rate-limit/assignments/{id}
POST   /api/rate-limit/assignments
PUT    /api/rate-limit/assignments/{id}
DELETE /api/rate-limit/assignments/{id}
```

## Enforcement Flow

For a database-backed gateway route:

1. Spring Cloud Gateway resolves the route.
2. The limiter resolves the active `gateway_routes` record by route key.
3. The limiter loads the highest-priority active route assignment.
4. Redis evaluates the policy counter for the route/source-IP scope.
5. Allowed requests continue to the upstream service.
6. Blocked requests return `429` before proxying upstream.
7. `request_logs` is written for routed traffic.
8. `rate_limit_decisions` is written when an assignment applies.
9. A `gateway.rate_limit.decision` Kafka event is recorded through the outbox.

## Algorithms

`SLIDING_WINDOW` uses a Redis sorted set per route/source-IP scope and removes entries outside the configured window before counting the current request.

`TOKEN_BUCKET` uses a Redis Lua script to atomically refill, consume, and persist bucket state.

`HYBRID` evaluates both sliding-window and token-bucket counters. A request is allowed only when both allow it.

The effective request limit is calculated from:

```text
floor(configured_limit / strictness_factor)
```

with a minimum of `1`.

## Database Connection

The rate limiter uses real PostgreSQL tables from Phase 2:

- `rate_limit_policies`
- `rate_limit_assignments`
- `request_logs`
- `rate_limit_decisions`

There is no process-local policy registry and no in-memory counter fallback.

## Redis Keys

Rate limiter counters use the Phase 6 Redis namespace:

```text
{ADAPTIVE_GATEWAY_REDIS_KEY_PREFIX}:rate-limit:sliding:{routeId}:{routeKey}:{sourceIp}
{ADAPTIVE_GATEWAY_REDIS_KEY_PREFIX}:rate-limit:token-bucket:{routeId}:{routeKey}:{sourceIp}
```

## Kafka Events

When a rate limit assignment applies, the backend records a Kafka outbox event:

```text
gateway.rate_limit.decision
```

The payload includes route id, route key, policy id, assignment id, algorithm, decision, effective limit, observed count, remaining tokens, and retry-after seconds.

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- Policy algorithm validation.
- Redis sliding-window allow/block behavior.
- Existing auth, gateway, Redis, and Kafka tests still pass.

Runtime endpoint and proxy verification still require real PostgreSQL, Redis, Kafka, and the required environment variables. No embedded Redis, Kafka, database, or in-memory limiter fallback was added.
