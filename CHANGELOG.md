# Changelog

Full-Stack Sync Routine adds one dated entry per run; the newest is at the
top. Product-facing changes belong in a release-notes file when it exists,
not here.

## 2026-08-29

- **Phase 0 bootstrap.** Created `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`. `docs/API.md` is now the canonical contract between this
  backend and the `conexus-mail-mobile` client — every future API change
  lands here in the same commit as the code.
- **First mobile-facing endpoint.** Added
  `GET /api/health` (`com.emailmessenger.api.ApiHealthController`)
  returning `{status, version, timestamp}`. Anonymous, sourced from
  Spring Boot's `BuildProperties` when available.
- **Security config.** Permit `/api/health` anonymously and exempt
  `/api/**` from CSRF — mobile clients cannot participate in the browser
  CSRF-token flow, so every other `/api/*` endpoint must authenticate
  its caller explicitly (documented in `docs/BACKEND.md`).
- **Related repo.** `CLAUDE.md` now names `conexus-mail-mobile` as the
  sibling and points to each doc's role.
- Tests: `mvn -Dtest=ApiHealthControllerTest test` → 1 passed.
