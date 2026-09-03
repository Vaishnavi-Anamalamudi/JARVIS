# Contributing

Thanks for improving Adaptive Gateway Platform.

## Development Setup

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

Run frontend checks:

```powershell
cd frontend
npm ci
npm run build
```

Validate Docker Compose:

```powershell
docker compose --env-file .env.example config --quiet
```

## Pull Request Expectations

- Keep changes scoped to one feature or fix.
- Add or update tests for behavior changes.
- Update docs when APIs, environment variables, deployment, or security posture changes.
- Do not commit secrets, `.env`, build output, `target/`, `node_modules/`, or IDE metadata.
- Use clear commit messages such as `feat: add alert resolution workflow` or `fix: revoke refresh token on logout`.

## Code Style

- Backend changes should stay inside the owning module when possible.
- API inputs and outputs should use DTOs.
- State-changing administrative actions should create audit logs.
- UI interactions should call real backend APIs and handle loading, empty, error, and success states.
