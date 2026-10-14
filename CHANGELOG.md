# Changelog

## 2026-10-14 — Phase 0 bootstrap (sync routine)
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this file; added "Related repositories" to `CLAUDE.md`.
- Findings: no mobile REST API exists; backend is JPA (not jOOQ); mobile repo was empty. No code changes.
- Gates: `mvn -q -B test` exit 0 (offline `-o` fails: empty local repo).
