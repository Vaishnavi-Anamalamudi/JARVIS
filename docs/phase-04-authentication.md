# Phase 4: Authentication

## Objective

Implement real PostgreSQL-backed authentication with JWT access tokens, durable refresh tokens, login audit records, password hashing, role-based access control, validation, exception handling, and Swagger documentation.

This phase does not implement gateway routing behavior, Redis, Kafka, rate limiting, adaptive learning, anomaly detection, frontend, WebSocket, analytics screens, or deployment.

## Added Backend Capabilities

- Spring Security for WebFlux.
- JWT resource-server validation.
- HMAC SHA-256 JWT issuing through Spring Security OAuth2 JOSE.
- BCrypt password hashing.
- PostgreSQL-backed users, roles, refresh tokens, and login audit logs.
- First administrator setup flow guarded by an empty user table.
- Admin-only user registration.
- Login with username or email.
- Refresh-token rotation.
- Logout refresh-token revocation.
- Current-user profile endpoint.
- Bearer authentication in OpenAPI.
- Structured JSON errors for unauthenticated and forbidden requests.

## REST Endpoints

Public endpoints:

```text
POST /api/auth/setup-admin
POST /api/auth/login
POST /api/auth/refresh
```

Protected endpoints:

```text
GET  /api/auth/me
POST /api/auth/logout
POST /api/auth/users
```

`POST /api/auth/users` requires the configured administrator authority.

## Database Connection

The authentication implementation writes to real PostgreSQL tables from Phase 2:

- `roles`
- `app_users`
- `refresh_tokens`
- `login_audit_logs`

There is no process-local user store and no embedded database profile.

## Environment Variables

Phase 4 adds these required variables:

```text
ADAPTIVE_GATEWAY_ADMIN_ROLE_NAME
JWT_ISSUER
JWT_SECRET_BASE64
JWT_ACCESS_TOKEN_TTL_MINUTES
JWT_REFRESH_TOKEN_TTL_DAYS
```

`JWT_SECRET_BASE64` must decode to at least 32 bytes.

## First Administrator Flow

`POST /api/auth/setup-admin` works only while the active user count is zero.

The request role must match `ADAPTIVE_GATEWAY_ADMIN_ROLE_NAME`. The role is created from the request if it does not already exist, then the administrator user is created with a BCrypt password hash, login audit is persisted, and JWT tokens are returned.

After the first active user exists, this endpoint returns a conflict error.

## Verification

Verified in this phase:

- Backend compilation.
- Unit test execution.
- Static scan for forbidden implementation markers.
- Security and auth classes compile against Spring Boot 4 and Spring Security 7.

Runtime endpoint verification still requires a real PostgreSQL instance plus the required environment variables. No embedded database fallback was added.
