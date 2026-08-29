# CHANGELOG — email-messenger

The pre-existing `master/CHANGELOG.md` is the record of the prior
single-repo "master" routine, which shipped EPIC-02 through EPIC-18.
This file is the new record for the cross-repo sync routine that keeps
the backend and the sibling `conexus-mail-mobile` client in step.

Newest first.

## 2026-08-29

**Phase 0 bootstrap of the cross-repo sync routine.** No product code
changed. Added the doc scaffolding the routine requires:

- `docs/BACKEND.md` — architecture, tech stack, package layout, data
  model, auth model, API-surface note (there isn't one yet), scheduled
  jobs. Reverse-engineered from the current tree.
- `docs/API.md` — the canonical contract file. Records that no REST
  API exists yet and lays out the conventions any future endpoint MUST
  follow (base path, auth, media type, error shape, IDs, pagination,
  server authority). Endpoint inventory intentionally empty.
- `docs/WEB.md` — Thymeleaf page inventory grouped by controller,
  including shared fragments and the rule that web and API must share
  services.
- `INTERNAL_TODO.md` — top item is the mobile-strategy decision, which
  is a human call: adding a mobile REST surface silently on autopilot
  would break server authority and mispair with the existing PWA plan.
- `CLAUDE.md` — added §"Related repositories" pointing at the sibling
  Flutter checkout and each doc's role.

Backend code, migrations, tests, and the Thymeleaf site are untouched.
