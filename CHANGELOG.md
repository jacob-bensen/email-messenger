# Changelog — email-messenger

Append-only log for the autonomous full-stack routine. Human-authored
commits are noted here only if they change the API contract.

## 2026-08-28

Phase 0 bootstrap (autonomous routine, first run).

- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` — the canonical
  architecture, wire contract, and Thymeleaf page inventory.
- Added `INTERNAL_TODO.md` — running gap list.
- New REST endpoint `GET /api/v1/version` (`api/ApiVersionController`).
  Unauthenticated smoke test the mobile client hits at startup to
  confirm reachability and inspect `apiVersion` (currently `1`).
- `SecurityConfig`: permit `/api/v1/version`, and CSRF-exempt the whole
  `/api/**` block since the mobile API will use bearer tokens once
  auth ships.
- Added `src/test/java/com/emailmessenger/api/ApiVersionControllerTest`
  (MockMvc, mirrors `ActuatorHealthEndpointTest`).
- Added `CLAUDE.md` "Related repositories" section pointing at
  `conexus-mail-mobile`.
