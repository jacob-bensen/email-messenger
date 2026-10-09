# Changelog

## 2026-10-09
- Phase 0 bootstrap of the two-repo sync docs: added `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`, `INTERNAL_TODO.md`, and a "Related repositories" section in `CLAUDE.md`.
  Docs only; no code or schema changes. Sync audit found no mobile REST API/token auth
  (mobile repo is an empty scaffold). `mvn -q -B verify` exit 0.
