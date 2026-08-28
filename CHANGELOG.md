# CHANGELOG

Dated entries per full-stack sync routine run. Newest first.

## 2026-08-28 — Phase 0 bootstrap

- Added `docs/BACKEND.md` (module map, domain, DB, auth, email pipeline).
- Added `docs/API.md` — honest current state: no JSON REST API for the
  mobile client yet; only the Stripe webhook and the `/chats/events` SSE
  stream (session-cookie authed). Planned mobile endpoints listed as TBD.
- Added `docs/WEB.md` — full Thymeleaf page inventory + controller map.
- Added `CHANGELOG.md` (this file) and `INTERNAL_TODO.md`.
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  `jacob-bensen/conexus-mail-mobile`, and a "Shared docs" list.
- No source code or Flyway migrations changed. Tests not re-run — no
  behavior change.

### Discovery

- ORM is **JPA/Hibernate**, not jOOQ as the generic routine assumed.
  Docs and INTERNAL_TODO reflect reality.
- Only `POST /billing/webhook` returns/consumes JSON. Everything else is
  form-encoded + HTML or SSE.
- `conexus-mail-mobile` sibling repo is empty (just a README). Bootstrapped
  in a matching commit there.
