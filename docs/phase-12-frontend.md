# Phase 12: Frontend

## Objective

Build the React TypeScript operations console on top of the completed REST APIs so administrators can operate the adaptive gateway from a browser.

This phase does not implement WebSocket delivery, dedicated analytics rollup screens, export workflows, or deployment packaging.

## Added Frontend Capabilities

- Vite React TypeScript application under `frontend/`.
- Material UI operations shell with responsive desktop drawer and mobile navigation.
- Redux Toolkit authentication state with local token persistence.
- Axios API client with bearer token injection and standard API envelope handling.
- Login flow backed by `/api/auth/login` and session validation backed by `/api/auth/me`.
- Protected routes for authenticated administrators.
- Dashboard overview using system health, routes, request history, alerts, and route metrics.
- Gateway management screens for upstream services and routes.
- Rate-limit management screens for policies and route assignments.
- API consumer management and one-time credential creation display.
- Operations screens for gateway request history and audit logs.
- Adaptive learning screens for route metrics, heatmap buckets, adjustments, and manual learning runs.
- Anomaly and alert screens for anomaly records, stat snapshots, alerts, and manual detection runs.
- Production build chunking for React/Redux, MUI/Emotion, Recharts, and vendor code.

## Required Installations

The frontend dependency toolchain has been installed and locked through `frontend/package-lock.json`.

Installed and verified key packages:

```text
vite@8.2.2
@vitejs/plugin-react@6.1.0
react-router-dom@7.18.2
recharts@3.10.1
@types/node@26.3.0
```

## Environment Variables

Phase 12 adds the optional frontend environment variable:

```text
VITE_API_BASE_URL
VITE_BACKEND_PROXY_TARGET
```

When `VITE_API_BASE_URL` is omitted, the browser uses the current origin. When `VITE_BACKEND_PROXY_TARGET` is omitted, Vite proxies `/api` requests to:

```text
http://localhost:8080
```

## REST API Connection

The frontend calls the completed backend REST API surface:

```text
/api/auth
/api/system/health
/api/gateway
/api/rate-limit
/api/consumers
/api/operations
/api/adaptive-learning
/api/anomalies
/api/alerts
```

No mock API layer, in-memory admin dataset, or static JSON fixture is used.

## Build Output

The production bundle is generated under `frontend/dist/`, which remains ignored by Git.

The Vite build is configured with manual chunks:

- `react`
- `mui`
- `charts`
- `vendor`

## Verification

Verified in this phase:

- Frontend dependencies installed.
- Frontend lockfile generated.
- Known frontend dependency vulnerabilities resolved.
- TypeScript app code type-checks.
- Vite config type-checks.
- Production frontend build succeeds.
- Backend Phase 1-11 test suite still passes.

Runtime endpoint verification still requires the backend and its real PostgreSQL, Redis, Kafka, and environment variables.
