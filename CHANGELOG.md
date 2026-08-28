# Changelog

Dated entries for backend changes worth calling out. Newest first.

## 2026-08-28

- Bootstrapped the `docs/` system: `BACKEND.md`, `API.md` (canonical
  contract), `WEB.md`. Added `CHANGELOG.md` and `INTERNAL_TODO.md`.
  `CLAUDE.md` now has a "Related repositories" section pointing at
  `conexus-mail-mobile`.
- Added the first JSON API endpoint: `GET /api/v1/health`. Returns
  `{status, version, serverTime}`. Permitted in `SecurityConfig` and
  exempted from CSRF alongside the rest of `/api/v1/**` (the API is
  stateless).
- Exposed `spring.application.version` in `application.yml` so the
  health endpoint can report it.
