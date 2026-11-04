# CHANGELOG

## 2026-11-04 — sync-routine bootstrap (docs only)
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`,
  this file; added "Related repositories" to `CLAUDE.md`.
- Why: first routine run; the repo had no docs. Audit found no mobile REST API
  and an empty mobile repo, so no feature was implemented this run.
- No code or migration changes. `./mvnw -q -B test`: exit 0 (quiet mode; no failures reported).
