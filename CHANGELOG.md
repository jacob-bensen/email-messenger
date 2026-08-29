# CHANGELOG

Cross-repo sync log for the full-stack sync routine. Product-launch and
operator work has its own log at `master/CHANGELOG.md`; keep this one
for changes made in tandem with the mobile client
(`conexus-mail-mobile`).

## 2026-08-29 — Phase 0 bootstrap

- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`.
- Added `INTERNAL_TODO.md`.
- Recorded the sync-audit finding that no REST API exists yet — every
  user-facing route serves HTML, and the mobile repo is a bare
  `README.md`. Both sides are documented as TBD in `docs/API.md`.
- Added "Related repositories" section to `CLAUDE.md`.

No code changes. Next run should pick an item from `INTERNAL_TODO.md`
(top of the list is choosing the mobile-client auth scheme and shipping
`POST /api/v1/auth/login` end-to-end).
