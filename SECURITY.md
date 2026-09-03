# Security Policy

## Supported Versions

This repository currently supports the `main` branch.

## Reporting A Vulnerability

Do not open a public issue for a sensitive vulnerability. Use a private maintainer contact channel or GitHub private vulnerability reporting if it is enabled for the published repository.

Include:

- Affected component.
- Steps to reproduce.
- Impact.
- Suggested mitigation, if known.

## Secret Handling

Never commit real values for:

- Database credentials.
- JWT secrets.
- API keys.
- Refresh tokens.
- Private certificates.
- Cloud credentials.

Use `.env.example` only for non-production templates. Replace all example secrets before deployment.

## Security Checks Before Release

- `backend`: `.\mvnw.cmd test`
- `frontend`: `npm audit --audit-level=moderate`
- `frontend`: `npm run build`
- `docker compose --env-file .env.example config --quiet`

## Known Security Roadmap

- Account lockout and login throttling.
- Separate operator, analyst, and administrator roles.
- Dependency and container vulnerability scanning in CI.
- TLS/reverse-proxy hardening guide for a selected cloud platform.
