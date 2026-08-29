# Changelog

## 2026-08-29 — Phase 0 bootstrap (full-stack sync routine)

Not a feature run. The full-stack sync routine ran for the first time
across `email-messenger` (this repo, backend) and its sibling
`conexus-mail-mobile` (Flutter client). Both were missing the doc system
the routine depends on, and the mobile repo held only a README.

- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` — first
  cross-repo contract.
- Added `INTERNAL_TODO.md` and this `CHANGELOG.md`.
- Updated `CLAUDE.md` with a "Related repositories" section.
- No code, migrations, or endpoints changed. The single existing JSON
  endpoint (`POST /chats/refresh`) and SSE stream
  (`GET /chats/events`) are documented; everything else the mobile
  client will need is documented as target-state under `/api/v1/**`
  with a status: bootstrap warning.

## Prior history

Only `master/CHANGELOG.md` existed before this run — a dense
project-owner log of feature milestones (EPIC-17, EPIC-18, live
delivery IDLE + SSE, chat rewrite, etc.). It's not a repo-level
changelog and stays where it is. This file starts fresh for the
full-stack sync routine's cross-repo entries.
