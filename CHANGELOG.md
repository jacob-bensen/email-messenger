# Changelog — email-messenger (backend)

Newest first. Each entry is dated and short — one bullet per repo change.

## 2026-08-29

- Bootstrapped the shared docs system: `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`. Updated `CLAUDE.md`
  with a "Related repositories" section pointing at
  `conexus-mail-mobile`.
- Added the JSON REST v1 surface: `GET /api/v1/health`,
  `GET /api/v1/version`, `GET /api/v1/plans` under
  `com.emailmessenger.api`. Whitelisted `/api/v1/**` in `SecurityConfig`
  (public read-only for now). Slice-tested with `ApiV1ControllerTest`.
