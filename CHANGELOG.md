# Changelog

Dated entries per autonomous run. Not a semver product changelog —
this tracks what the routine touched and why.

## 2026-08-28

- Bootstrapped the doc system requested by the full-stack sync
  routine.
- Added `docs/BACKEND.md` — module map, persistence, auth, real-time
  delivery, testing.
- Added `docs/API.md` — canonical mobile REST contract. As of today
  no `/api/v1/*` endpoint is implemented on the server; the file
  documents the target shape and marks each entry PLANNED vs
  IMPLEMENTED.
- Added `docs/WEB.md` — Thymeleaf page inventory linking URL →
  template → controller → service.
- Added `INTERNAL_TODO.md` — ordered gap list. Top item: no REST
  API exists for the mobile client; every entry in `docs/API.md`
  needs building.
- Extended `CLAUDE.md` with a "Related repositories" section
  pointing at `conexus-mail-mobile`.
