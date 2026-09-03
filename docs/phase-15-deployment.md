# Phase 15: Deployment

## Objective

Package the full adaptive gateway platform for local production-style deployment with Docker Compose, repeatable environment configuration, health checks, and CI verification.

This phase completes the planned phase sequence.

## Added Deployment Capabilities

- Backend multi-stage Dockerfile.
- Frontend multi-stage Dockerfile.
- Nginx production frontend serving.
- Same-origin reverse proxy for backend REST APIs.
- Nginx WebSocket proxying for `/api/live/ws`.
- Docker Compose stack for:
  - PostgreSQL
  - Redis
  - Kafka
  - Spring Boot backend
  - React/Nginx frontend
- Persistent Docker volumes for PostgreSQL, Redis, and Kafka.
- Container health checks for infrastructure, backend, and frontend.
- Root `.env.example` covering all required backend, frontend, and compose variables.
- Frontend and backend Docker build contexts with `.dockerignore` files.
- GitHub Actions CI for backend tests, frontend install/audit/build, and compose config validation.
- PowerShell deployment smoke-test script.

## Deployment Files

```text
docker-compose.yml
.env.example
backend/Dockerfile
backend/.dockerignore
frontend/Dockerfile
frontend/.dockerignore
frontend/nginx.conf
.github/workflows/ci.yml
scripts/deployment-smoke-test.ps1
```

## Runtime Services

The compose stack exposes:

```text
Frontend:   http://localhost:${FRONTEND_PORT}
Backend:    http://localhost:${BACKEND_PORT}
PostgreSQL: localhost:${POSTGRES_PORT}
Redis:      localhost:${REDIS_PORT}
Kafka:      localhost:${KAFKA_PORT}
```

With the provided `.env.example`, the default frontend URL is:

```text
http://localhost:8088
```

## Deployment Commands

Prepare a local environment file:

```powershell
Copy-Item .env.example .env
```

Build and start the stack:

```powershell
docker compose --env-file .env up -d --build
```

Check service health:

```powershell
docker compose --env-file .env ps
```

Run the smoke test:

```powershell
.\scripts\deployment-smoke-test.ps1 -BaseUrl http://127.0.0.1:8088
```

Stop the stack:

```powershell
docker compose --env-file .env down
```

Remove persistent volumes when a clean database, Redis store, and Kafka log are required:

```powershell
docker compose --env-file .env down -v
```

## Required Secret Rotation

Before using this outside local development, replace these values in `.env`:

```text
POSTGRES_PASSWORD
JWT_SECRET_BASE64
```

`JWT_SECRET_BASE64` must decode to at least 32 bytes.

## Verification

Verified in this phase:

- Docker and Docker Compose are installed.
- Compose configuration resolves with `.env.example`.
- Compose service list resolves to PostgreSQL, Redis, Kafka, backend, and frontend.
- Example JWT secret satisfies the backend minimum length.
- Backend compilation and full unit test suite pass.
- Frontend TypeScript build and production Vite bundle pass.
- Frontend npm audit reports zero vulnerabilities.
- Frontend dev server responds with HTTP 200.

Docker image build/start verification could not be completed because Docker Desktop's Linux engine was not running on this machine.

## Runtime Verification Requirement

Full runtime verification requires Docker Desktop or another Docker Engine to be running. Once Docker Engine is available, run:

```powershell
docker compose --env-file .env up -d --build
.\scripts\deployment-smoke-test.ps1 -BaseUrl http://127.0.0.1:8088
```
