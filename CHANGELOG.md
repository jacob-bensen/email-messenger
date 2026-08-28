# CHANGELOG

Human-readable notes on what changed in this repo run-to-run.

## 2026-08-28

- **Bootstrap docs** (`docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`) as the
  shared contract with the ConexusMail mobile client.
- **New REST endpoint** `GET /api/v1/health` returning
  `{ status, version, serverTimeUtc }`, permitted through Spring Security so
  the mobile client can probe availability + backend version without auth.
  Slice test in `com.emailmessenger.api.HealthControllerTest`.
- Added `INTERNAL_TODO.md` for cross-repo tech debt tracking.
