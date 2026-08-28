# Changelog — email-messenger

Root-level changelog for full-stack sync runs. The per-epic engineering
log lives in `master/CHANGELOG.md`; this file tracks Phase 0 / Phase 1
sync work that spans the backend and the `conexus-mail-mobile` client.

## 2026-08-28 — Phase 0 bootstrap

- Identified this repo as the BACKEND and the sibling `conexus-mail-mobile`
  as the (empty) FRONTEND per the full-stack sync routine.
- Created `docs/BACKEND.md` — architecture, stack (JPA, not jOOQ), package
  map, domain model, auth model.
- Created `docs/API.md` — recorded that no JSON API exists today, defined
  the v1.0.0 `/api/v1/**` target contract that both repos will build to.
- Created `docs/WEB.md` — full Thymeleaf page inventory keyed by route,
  template, and the services each page must delegate to.
- Created `INTERNAL_TODO.md` with the audit findings ordered by severity.
- Added the "Related repositories" section to `CLAUDE.md`.
- No code changes on the backend this run.
