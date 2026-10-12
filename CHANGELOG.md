# CHANGELOG (full-stack sync routine)

Earlier per-feature history lives in `master/CHANGELOG.md`.

## 2026-10-12 — Phase 0 bootstrap
- Classified repos: this = BACKEND; `conexus-mail-mobile` = FRONTEND (README only).
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`,
  this file; appended "Related repositories" to `CLAUDE.md`.
- Sync audit result: backend has no REST API for a mobile client; recorded in
  `INTERNAL_TODO.md`. No code changed.
- Backend `./mvnw -q -B test`: exit 0 — 794 tests, 0 failures, 0 errors, 1 skipped (Docker-only).
