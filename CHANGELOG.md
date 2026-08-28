# Changelog — email-messenger

Newest first. One dated entry per autonomous run.

## 2026-08-28

- Bootstrapped the docs system for the two-repo (backend + mobile
  client) product:
  - `docs/BACKEND.md` — architecture, package layout, auth model,
    web ↔ API service-reuse rule.
  - `docs/API.md` — canonical REST contract for the mobile client
    (`conexus-mail-mobile`). Ground rules (bearer auth, JSON,
    ISO-8601 UTC, stable error slugs) plus the first live endpoint
    `GET /api/version`.
  - `docs/WEB.md` — Thymeleaf page inventory and controller ↔
    service map so the mobile REST controllers can reuse the same
    services rather than fork logic.
- Added `INTERNAL_TODO.md` tracking known contract gaps between the
  backend and the (fresh) mobile client.
- Extended `CLAUDE.md` with a "Related repositories" section
  pointing at `conexus-mail-mobile` and each doc's role.
- Added the first mobile REST endpoint: `GET /api/version` in a
  new `com.emailmessenger.api` package, permitted anonymously in
  `SecurityConfig`, wired to `spring.application.name` and Maven
  `${project.version}`. Ships with a controller test.
