# CHANGELOG

(Older per-epic history lives in `master/CHANGELOG.md`.)

## 2026-10-27
- Quality gates: `mvn verify` not run (docs-only change).
- Phase 0 bootstrap of the two-repo sync docs: added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, and a "Related repositories" section in `CLAUDE.md`. Docs only; no code changes.
- Sync audit finding: backend exposes no JSON API for a mobile client, and the mobile repo is empty — see `INTERNAL_TODO.md`.
