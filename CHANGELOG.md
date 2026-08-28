# CHANGELOG — email-messenger

Append a dated entry each run of the full-stack sync routine. Newest at
top.

## 2026-08-28 — Phase 0 bootstrap

- Created `docs/BACKEND.md` — architecture overview: stack, package
  layout, domain model, auth model, how Thymeleaf pages relate to the
  future REST API, persistence + migrations, scheduled jobs, config.
- Created `docs/API.md` — canonical JSON API contract for the
  `conexus-mail-mobile` client. Documented current status (no `/api/v1`
  exists yet), conventions (base path, content-type, error shape,
  pagination, idempotency), planned auth model (bearer JWT + refresh),
  and the planned surface (session/account, mailboxes, chats, search,
  live events, billing). All endpoints marked `TBD`.
- Created `docs/WEB.md` — full Thymeleaf page inventory grouped by
  controller, mapping every route to its template and service. Reserved
  `/api/**` for the mobile REST API.
- Created `INTERNAL_TODO.md` — severity-ordered gap list. Top item:
  no `/api/v1` at all — start with token-based auth.
- Created this `CHANGELOG.md`.
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  `conexus-mail-mobile`.
- No code changes; no migrations added; test suite not re-run.
