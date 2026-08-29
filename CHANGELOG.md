# CHANGELOG

Dated entries per full-stack-sync run. Newest first.

## 2026-08-29 — Phase 0 bootstrap + first mobile-facing endpoint

- Bootstrapped the doc system for the full-stack-sync routine:
  `docs/BACKEND.md` (module layout, data, auth model, web-vs-REST split),
  `docs/API.md` (canonical REST v1 contract; single source of truth
  shared with the mobile client), `docs/WEB.md` (Thymeleaf page inventory).
- Added `CHANGELOG.md` and `INTERNAL_TODO.md`.
- Updated `CLAUDE.md` with a "Related repositories" pointer to the
  ConexusMail mobile client and to the new doc set.
- Added `com.emailmessenger.api` package with `AppInfoController` serving
  `GET /api/v1/app-info` — the first REST endpoint aimed at the mobile
  client. Reads `Clock` to produce `serverTime` (no persistence). Unit
  test in `src/test/java/com/emailmessenger/api/AppInfoControllerTest.java`.
- Contract in `docs/API.md` matches the shipped endpoint exactly.
