# Project Audit

## Existing Project Shape

- Frontend: React TypeScript app under `frontend/` using Vite, Material UI, Redux Toolkit, Axios, and Recharts.
- Backend: Java 21 Spring Boot modular monolith under `backend/`.
- Gateway: Spring Cloud Gateway with database-backed route definitions.
- Database: PostgreSQL schema managed by Flyway.
- Authentication: JWT access tokens, durable refresh tokens, BCrypt password hashing, role-based admin access.
- Runtime infrastructure: Redis for counters/session cache and Kafka for operational event publishing/outbox records.
- Real-time: JWT-protected WebSocket stream under `/api/live/ws`.
- DevOps: Dockerfiles, Docker Compose, Nginx frontend proxy, GitHub Actions CI, smoke-test script.
- Tests: backend service tests and frontend production build checks.
- Documentation: phase docs plus publication-ready README and operational docs.

## Strengths

- The system preserves a credible product idea: adaptive API gateway operations and traffic-aware rate limiting.
- Backend features are connected through real persistence rather than mock datasets.
- The frontend is a real authenticated operations console, not a static template.
- Flyway schema includes constraints, indexes, timestamps, soft deletion, and audit tables.
- Tests cover major backend services, including auth, rate limits, analytics, adaptive learning, anomaly detection, Kafka, Redis, live events, and alerts.
- CI validates backend tests, frontend audit/build, and Docker Compose configuration.

## Issues Found

- Logout cleared client state but did not send the backend-required refresh token, so server-side refresh-token revocation could be skipped.
- Alerts were generated and listed but had no operator workflow for acknowledgement or resolution.
- The alert database schema had `acknowledged_by_user_id`, but the entity did not map it.
- README still read like phase tracking notes rather than a professional GitHub landing page.
- Required GitHub docs and community files were missing.
- Full Docker runtime could not be verified because Docker Desktop was not running.

## Improvements Applied

- Fixed logout to submit the stored refresh token.
- Prevented login failures from causing unnecessary redirect behavior.
- Added alert acknowledgement and resolution endpoints.
- Added alert actor attribution through the existing `acknowledged_by_user_id` column.
- Added audit logging and live events for alert status changes.
- Added React alert row actions with status-aware disabled states.
- Added backend tests for alert acknowledgement, resolution, and invalid state transition behavior.
- Replaced the README with a professional product README.
- Added architecture, API, database, security, deployment, development, and troubleshooting docs.
- Added Apache-2.0 license, contributing guide, security policy, code of conduct, issue templates, and pull request template.

## Residual Risks

- Docker runtime must be verified after Docker Desktop starts.
- Browser end-to-end tests are not yet present.
- Commit messages currently visible in Git history are generic and should be renamed only if the owner is comfortable rewriting already-published history.
- Multi-role authorization beyond the configured administrator role is a future enhancement.
