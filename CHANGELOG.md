# Changelog

One dated entry per full-stack sync run: what changed and why. Entries before
2026-11-11 are in [`master/CHANGELOG.md`](master/CHANGELOG.md).

## 2026-11-11 — Phase 0: docs bootstrap and sync audit

First run of the two-repo sync routine. No application code changed.

- Added `docs/BACKEND.md` (architecture, data model, business rules, auth
  model), `docs/API.md` (the canonical contract), and `docs/WEB.md` (page
  inventory), all reverse-engineered from the current code.
- Added `INTERNAL_TODO.md` with the sync audit, ordered by severity.
- Added a "Related repositories" section and a stack note to `CLAUDE.md`.

Why: the mobile client repo (`conexus-mail-mobile`) is empty and the backend
has no JSON API, so there was no contract to sync. The audit's main findings
are that the mobile client needs token auth and a `/api/v1` surface that do not
exist, and that chat send logic sits in `ConversationController` where an API
controller could not reuse it.

Verification: `mvn -q -B verify` exited 0 before the docs were added — 794
tests, 0 failures, 0 errors, 1 skipped.
