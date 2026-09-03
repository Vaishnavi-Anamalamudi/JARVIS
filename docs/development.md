# Development

## Repository Layout

- `backend/`: Spring Boot API gateway runtime.
- `frontend/`: React TypeScript operations console.
- `docs/`: architecture, phase notes, deployment, API, and security documentation.
- `.github/workflows/ci.yml`: build and validation pipeline.
- `scripts/`: operational helper scripts.

## Backend

Run tests:

```powershell
cd backend
.\mvnw.cmd test
```

Important packages:

- `com.adaptivegateway.auth`
- `com.adaptivegateway.gateway`
- `com.adaptivegateway.ratelimit`
- `com.adaptivegateway.analytics`
- `com.adaptivegateway.adaptive`
- `com.adaptivegateway.anomaly`
- `com.adaptivegateway.alerts`
- `com.adaptivegateway.operations`

## Frontend

Install dependencies and build:

```powershell
cd frontend
npm ci
npm run build
```

Local Vite development proxies `/api` and WebSocket traffic to `VITE_BACKEND_PROXY_TARGET`, defaulting to `http://localhost:8080`.

## Coding Standards

- Keep feature logic inside the owning backend module.
- Use DTOs for API input/output.
- Validate request bodies and query limits.
- Persist security-relevant state changes through audit logs.
- Prefer backend-derived analytics over client-generated values.
- Add tests for service behavior that changes persisted state or security posture.

## Performance Notes

- Use pagination for list endpoints.
- Keep Redis operations bounded and route-scoped.
- Keep analytics generation windowed.
- Add database indexes before adding high-cardinality query paths.
- Avoid frontend tables that render unbounded datasets.
