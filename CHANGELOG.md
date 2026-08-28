# Changelog

Dated entries appended per full-stack sync run.

## 2026-08-28

- **Phase 0 bootstrap.** Classified this repo as the BACKEND of the
  two-repo ConexusMail application. The sibling
  `../conexus-mail-mobile` is the Flutter mobile client. Created
  `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, and this file plus
  `INTERNAL_TODO.md`. Added a "Related repositories" section to
  `CLAUDE.md`.
- **First mobile-facing API endpoints.** Introduced
  `GET /api/v1/health` and `GET /api/v1/version` as the initial public
  REST contract so the mobile client has a real endpoint to call.
  `SecurityConfig` opens `/api/v1/**` for the two anonymous endpoints
  (authenticated paths will opt back into auth as they land). Wired
  through a new `PublicApiController` under `com.emailmessenger.web`
  with a slice test.
- **Contract note.** The generic sync routine assumes jOOQ; this repo
  uses JPA. Documented in `docs/BACKEND.md` and left the codebase
  unchanged.
