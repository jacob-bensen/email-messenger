# CHANGELOG — email-messenger

Dated log of routine runs. Newest first.

## 2026-08-31 — Phase 0 doc bootstrap for full-stack sync

- Classified repos: `email-messenger` = BACKEND (Spring Boot 3 + Thymeleaf +
  JPA + Flyway), `conexus-mail-mobile` = FRONTEND (Flutter placeholder).
- Created `docs/BACKEND.md`, `docs/WEB.md`, `docs/API.md` describing the
  current state accurately.
- Noted that today the backend has ONLY one JSON endpoint useful to a
  mobile client (`POST /chats/refresh`) plus the Stripe webhook and the
  `/chats/events` SSE stream — everything else is Thymeleaf.
- `docs/API.md` drafts the `/api/v1/*` contract; endpoints beyond the
  three above are DESIGN, not yet implemented. Owner decision needed on
  the auth scheme (recommendation: bearer tokens).
- Added `CHANGELOG.md`, `INTERNAL_TODO.md`, and a "Related repositories"
  section pointing at `conexus-mail-mobile`.
- No Java code changed. `./mvnw test` not re-run (Phase 0 is doc-only).
