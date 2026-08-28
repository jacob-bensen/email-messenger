# Changelog — email-messenger backend

Sync-routine entries only. Feature-level history is in git.

## 2026-08-28 — Phase 0 doc bootstrap

- Added `docs/BACKEND.md` (architecture, package layout, domain model).
- Added `docs/API.md` (draft — enumerates the ~3 JSON endpoints that
  exist today and lays out the full planned mobile REST surface as
  TBD placeholders).
- Added `docs/WEB.md` (Thymeleaf page inventory, DTOs, error mapping,
  auth matrix).
- Added `INTERNAL_TODO.md` — running gap list; the biggest item is
  "no REST API for the mobile client exists yet".
- Added `CHANGELOG.md` (this file).
- Appended a "Related repositories" section to `CLAUDE.md` pointing at
  the mobile repo and the doc split.
- No production code changed. No migrations added.
