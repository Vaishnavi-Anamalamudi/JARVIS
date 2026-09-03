# Troubleshooting

## PowerShell Blocks `npm`

Use `npm.cmd`:

```powershell
npm.cmd run build
```

## Backend Fails On Missing Environment Variables

The default profile expects environment variables. For Docker, copy `.env.example` to `.env` and run Compose with `--env-file .env`.

## JWT Secret Error

`JWT_SECRET_BASE64` must decode to at least 32 bytes. Generate a new secret before shared or production deployment.

## Database Validation Fails

The backend uses Hibernate validation instead of schema generation. Confirm Flyway ran successfully and the database matches `V1__initial_database_schema.sql`.

## Frontend Cannot Reach Backend

For Vite dev mode, set:

```text
VITE_BACKEND_PROXY_TARGET=http://localhost:8080
```

For Docker, the Nginx config proxies `/api` and WebSocket traffic to the backend service.

## WebSocket Shows Offline

Check that:

- The user is logged in with a valid JWT.
- `/api/live/ws` is reachable through the same origin or configured API base URL.
- The backend has not rejected the token.

## Docker Compose Does Not Start

Verify Docker Desktop or another Docker Engine is running, then validate config:

```powershell
docker compose --env-file .env.example config --quiet
```
