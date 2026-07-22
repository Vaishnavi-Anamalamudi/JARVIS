# Phase 11: REST APIs

## Objective

Expand the backend REST API surface needed by later frontend, dashboard, and operations phases while keeping every endpoint connected to real PostgreSQL-backed data.

This phase does not implement frontend screens, WebSocket delivery, analytics rollup jobs, export file generation, or deployment.

## Added Backend Capabilities

- API consumer management backed by `api_consumers`.
- API consumer credential management backed by `api_consumer_credentials`.
- One-time raw API key return on credential creation.
- SHA-256 credential hashing before persistence.
- Credential revocation.
- Gateway request history APIs backed by `request_logs`.
- Request detail responses including persisted rate limit decision data from `rate_limit_decisions`.
- Audit log persistence backed by `audit_logs`.
- Audit log read APIs for administrative review.
- Admin-only security rules for consumer and operations APIs.
- OpenAPI annotations on new controllers.

## REST Endpoints

All endpoints require the configured administrator authority:

```text
GET    /api/consumers
GET    /api/consumers/{id}
POST   /api/consumers
PUT    /api/consumers/{id}
DELETE /api/consumers/{id}

GET    /api/consumers/{consumerId}/credentials
POST   /api/consumers/{consumerId}/credentials
POST   /api/consumers/credentials/{credentialId}/revoke

GET    /api/operations/requests
GET    /api/operations/requests/{id}
GET    /api/operations/audit-logs
```

## Credential Behavior

Credential creation returns the raw API key only once in the response body. The database stores:

- key prefix
- SHA-256 credential hash
- expiration timestamp
- revoked timestamp
- audit timestamps

No raw credential value is persisted.

## Audit Behavior

Phase 11 records audit logs for:

- API consumer creation
- API consumer update
- API consumer deletion
- API credential creation
- API credential revocation

Each audit record includes actor, action, resource type, resource id, correlation id, IP address, user agent, and JSON metadata.

## Database Connection

This phase uses real PostgreSQL tables from Phase 2:

- `api_consumers`
- `api_consumer_credentials`
- `request_logs`
- `rate_limit_decisions`
- `audit_logs`

There is no in-memory API registry, mock credential store, or static request history.

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- API credential creation returns the raw key once.
- API credential persistence stores a SHA-256 hash, not the raw key.
- Existing Phase 1-10 tests still pass.

Runtime endpoint verification still requires real PostgreSQL, Redis, Kafka, and the required environment variables.
