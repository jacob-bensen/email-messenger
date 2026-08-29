# Changelog

Notable changes per full-stack-sync run. Reverse-chronological.

## 2026-08-29

- Bootstrap: added canonical docs — `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`. Updated
  `CLAUDE.md` with a "Related repositories" section pointing at
  `jacob-bensen/conexus-mail-mobile`.
- Feature: first mobile-facing REST endpoint,
  `GET /api/v1/health`, returning `{status, version, time}`. Public
  (no auth), CSRF-exempt for `/api/**`, unit + MVC slice test.
- No schema changes.
