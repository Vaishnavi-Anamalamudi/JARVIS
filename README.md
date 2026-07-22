# Adaptive API Gateway with Traffic-Aware Rate Limiting

Production-grade engineering project for an adaptive API gateway using Spring Boot, Spring Cloud Gateway, PostgreSQL, Redis, Kafka, WebSocket updates, and a React TypeScript dashboard.

## Current Status

Phase 1 is complete in this workspace: project architecture has been defined in `docs/phase-01-project-architecture.md`.

Phase 2 is complete in this workspace: database design has been defined in `docs/phase-02-database-design.md`, and the PostgreSQL Flyway migration is available at `backend/src/main/resources/db/migration/V1__initial_database_schema.sql`.

Phase 3 is complete in this workspace: backend foundation has been defined in `docs/phase-03-backend-foundation.md`, with the Spring Boot project under `backend/`.

Phase 4 is complete in this workspace: authentication has been defined in `docs/phase-04-authentication.md`, with PostgreSQL-backed auth code under `backend/src/main/java/com/adaptivegateway/auth` and security code under `backend/src/main/java/com/adaptivegateway/security`.

Phase 5 is complete in this workspace: gateway routing has been defined in `docs/phase-05-gateway.md`, with PostgreSQL-backed gateway configuration and dynamic Spring Cloud Gateway route loading under `backend/src/main/java/com/adaptivegateway/gateway`.

Phase 6 is complete in this workspace: Redis integration has been defined in `docs/phase-06-redis-integration.md`, with Redis health, namespaced keys, auth session caching, and temporary stats services under `backend/src/main/java/com/adaptivegateway/redis`.

Phase 7 is complete in this workspace: Kafka integration has been defined in `docs/phase-07-kafka-integration.md`, with Kafka producer health, request event publishing, and PostgreSQL-backed event outbox code under `backend/src/main/java/com/adaptivegateway/kafka`.

Phase 8 is complete in this workspace: rate limiting has been defined in `docs/phase-08-rate-limiter.md`, with PostgreSQL-backed policies and assignments, Redis counters, gateway enforcement, request logs, decision persistence, and Kafka decision events under `backend/src/main/java/com/adaptivegateway/ratelimit`.

Phase 9 is complete in this workspace: adaptive learning has been defined in `docs/phase-09-adaptive-learning.md`, with request-log aggregation, route metrics, heatmap buckets, strictness adjustments, and Kafka adjustment events under `backend/src/main/java/com/adaptivegateway/adaptive`.

Phase 10 is complete in this workspace: anomaly detection has been defined in `docs/phase-10-anomaly-detection.md`, with rolling statistic snapshots, anomaly records, alert creation, adaptive strictness increases, and Kafka anomaly events under `backend/src/main/java/com/adaptivegateway/anomaly` and `backend/src/main/java/com/adaptivegateway/alerts`.

Phase 11 is complete in this workspace: REST API expansion has been defined in `docs/phase-11-rest-apis.md`, with API consumer management, hashed API credential management, request history APIs, and audit log APIs under `backend/src/main/java/com/adaptivegateway/consumer` and `backend/src/main/java/com/adaptivegateway/operations`.

No frontend application, WebSocket integration, analytics screens, or deployment runtime has been generated yet. Those will be created phase by phase, only after review and confirmation.

## Required Phase Order

1. Project Architecture
2. Database Design
3. Backend Foundation
4. Authentication
5. Gateway
6. Redis Integration
7. Kafka Integration
8. Rate Limiter
9. Adaptive Learning Module
10. Anomaly Detection
11. REST APIs
12. Frontend
13. WebSocket
14. Analytics
15. Deployment

## Architecture Rule

Every runtime feature must follow this connection path:

Database -> Repository -> Service -> Business Logic -> REST API -> Frontend -> Live UI

Redis, Kafka, and WebSocket integrations are added only in their scheduled phases and then wired into completed features through verified, working paths.
