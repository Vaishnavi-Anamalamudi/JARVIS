# Phase 5: Gateway

## Objective

Implement PostgreSQL-backed gateway configuration and dynamic Spring Cloud Gateway routing.

This phase does not implement Redis, Kafka, rate limiting, adaptive learning, anomaly detection, API consumer credentials, frontend, WebSocket, analytics screens, request-log persistence, or deployment.

## Added Backend Capabilities

- JPA entities for `upstream_services`, `gateway_routes`, and `gateway_route_predicates`.
- Repositories for gateway configuration lookup and active route loading.
- Admin-only REST APIs for upstream service management.
- Admin-only REST APIs for gateway route management.
- Soft delete for gateway-managed upstreams and routes.
- Dynamic Spring Cloud Gateway `RouteDefinitionLocator` backed by PostgreSQL.
- Route refresh events after gateway configuration changes.
- Structured route predicates for `PATH`, `METHOD`, `HOST`, `HEADER`, and `QUERY`.
- Canonical `PATH` and `METHOD` predicates generated from route path and allowed methods.
- Pagination envelope for list endpoints.
- OpenAPI documentation annotations for gateway management endpoints.

## REST Endpoints

All endpoints require the configured administrator authority:

```text
GET    /api/gateway/upstreams
GET    /api/gateway/upstreams/{id}
POST   /api/gateway/upstreams
PUT    /api/gateway/upstreams/{id}
DELETE /api/gateway/upstreams/{id}

GET    /api/gateway/routes
GET    /api/gateway/routes/{id}
POST   /api/gateway/routes
PUT    /api/gateway/routes/{id}
DELETE /api/gateway/routes/{id}
POST   /api/gateway/routes/refresh
```

## Gateway Routing

Spring Cloud Gateway loads active route definitions from PostgreSQL through `DatabaseRouteDefinitionLocator`.

A route is routable when:

- `gateway_routes.status = ACTIVE`
- `gateway_routes.deleted_at IS NULL`
- its upstream service is not soft-deleted
- its upstream service status is `ACTIVE` or `DEGRADED`

Each active route is mapped to:

- route id: `gateway_routes.route_key`
- upstream URI: `upstream_services.base_url`
- order: `gateway_routes.priority`
- predicates: active `gateway_route_predicates`
- optional `StripPrefix` filter when `strip_prefix > 0`

## Database Connection

The gateway implementation uses real PostgreSQL tables from Phase 2:

- `upstream_services`
- `gateway_routes`
- `gateway_route_predicates`

There is no process-local route registry and no embedded database profile.

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- Gateway route creation creates canonical `PATH` and `METHOD` predicates.
- Active database routes map to Spring Cloud Gateway route definitions.
- Static scan for forbidden implementation markers.

Runtime endpoint and proxy verification still require a real PostgreSQL instance plus the required environment variables. No embedded database fallback was added.
