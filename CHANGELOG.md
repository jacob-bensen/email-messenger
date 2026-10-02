# CHANGELOG

## 2026-10-02 — Full-stack sync routine: bootstrap (Phase 0)
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` reverse-engineered from
  the code; `INTERNAL_TODO.md`; this file; "Related repositories" in `CLAUDE.md`.
- Sync audit finding: no REST API / token auth for the mobile client exists
  (see `INTERNAL_TODO.md` #1). No code changed.
- Verified: `mvn -q -B verify` exit 0.
