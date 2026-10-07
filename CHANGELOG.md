# Changelog

(Older entries from a previous routine live in `master/CHANGELOG.md`.)

## 2026-10-07
Phase 0 bootstrap (no feature): added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`,
and a "Related repositories" section in `CLAUDE.md`. Sync audit found no REST API and an empty mobile repo;
`docs/API.md` records only the existing machine-facing endpoints (`/chats/refresh`, `/chats/events`,
`/billing/webhook`) and lists open `/api/v1` decisions as TBD. No code or schema changed.
Tests: `mvn -B test` → 794 run, 0 failed, 1 skipped.
