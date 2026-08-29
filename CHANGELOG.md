# Changelog

Dated entries per full-stack-sync routine run.

## 2026-08-29 — doc system bootstrap

- Bootstrapped the doc system: created `docs/BACKEND.md` (module and
  domain overview, auth model), `docs/API.md` (shared REST contract —
  see the "Status: bootstrap" section), and `docs/WEB.md` (Thymeleaf
  page inventory of every controller).
- Added `INTERNAL_TODO.md` with the severity-ordered mismatches found
  during the sync audit.
- Added a "Related repositories" section to `CLAUDE.md` linking to the
  sibling mobile repo.
- **No feature work this run.** Phase 0 was substantial: the mobile
  repo was empty and the backend had zero docs. See
  `INTERNAL_TODO.md` for what's blocking the first end-to-end feature
  (short version: the backend has no JSON REST API for mobile clients
  to call).
