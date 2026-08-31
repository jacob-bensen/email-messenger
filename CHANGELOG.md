# Changelog — email-messenger (repo root)

This is the top-level changelog for the two-repo sync routine. The
detailed backend-feature log lives at `master/CHANGELOG.md` and
continues to serve the operator/product routine.

Entries here are dated and scoped to cross-repo work: docs bootstrap,
API contract changes, and coordinated changes with
`conexus-mail-mobile`.

## 2026-08-31

Phase 0 bootstrap of the full-stack sync routine.

- Added `docs/BACKEND.md` — concise architecture reference for this
  repo's role in the two-repo system.
- Added `docs/WEB.md` — Thymeleaf page inventory grouped by area,
  each row naming the controller + services so refactors can see at a
  glance whether they're touching business logic.
- Added `docs/API.md` — canonical REST contract for the mobile
  client. Legend marks every `/api/v1/**` endpoint 🚧 Planned since
  the JSON API layer has not been built yet; the two existing JSON
  endpoints (`POST /chats/refresh`, `POST /billing/webhook`) are
  captured as ✅.
- Added `INTERNAL_TODO.md` at the repo root — cross-repo sync
  backlog. Top item is "no mobile REST API exists in code" with
  first-feature guidance (`GET /api/v1/me` + `POST /api/v1/auth/login`).
- Added `CHANGELOG.md` (this file) for sync-scoped entries; the
  existing `master/CHANGELOG.md` is untouched.
- Updated `CLAUDE.md` with a "Related repositories" section pointing
  at `conexus-mail-mobile` and each new doc's role.
- Sync audit findings recorded in `INTERNAL_TODO.md` §Contract
  mismatches: zero mobile REST API, no bearer auth on `/api/**`, no
  CSRF exemption plan yet.
- No functional code changed this run — Phase 0 was substantial
  enough (docs system created from scratch, mobile-side scaffold
  created from an empty repo) that no feature was implemented.
  Backend tests were not re-run because no source code changed.
