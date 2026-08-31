# CHANGELOG

Dated log of changes made by the full-stack sync routine.

## 2026-08-31 — Phase 0 bootstrap

- Created `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` reverse-engineered
  from source. No behavior changes.
- Created `INTERNAL_TODO.md` capturing the gap between the current
  Thymeleaf-only server and the JSON API a mobile client will need.
- Added "Related repositories" section to `CLAUDE.md` pointing at the
  sibling `conexus-mail-mobile` repo.
- Sibling mobile repo (`conexus-mail-mobile`) was empty; scaffolded a
  minimal Flutter project and its doc set (`docs/DESIGN.md`,
  `docs/MOBILE.md`, `CLAUDE.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`).
- No code changes to the backend beyond the CLAUDE.md addition; the
  existing Maven test suite is untouched.
