# CHANGELOG

## 2026-11-07 — doc bootstrap (no code changes)
Created `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`; added "Related repositories" to CLAUDE.md.
Why: sync routine found no docs, no REST API and an empty mobile repo; documented reality and the planned `/api/v1` contract so the next run can implement it contract-first.
Baseline `mvn -q -B verify`: passed (exit 0).
