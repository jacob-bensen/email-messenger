# Changelog

Each dated entry is one autonomous full-stack sync run.

## 2026-08-30 — Phase 0 doc bootstrap
- Created `docs/BACKEND.md` documenting modules, persistence, auth,
  mailbox pipeline, billing, and scheduled jobs from the current code.
- Created `docs/API.md` as the shared contract SSoT. Recorded that the
  mobile-facing JSON REST API does **not exist yet** and enumerated the
  entire current endpoint surface (Thymeleaf form-post + SSE +
  `/billing/webhook`). Sketched the planned `/api/v1/*` shape as TBD so
  it can be implemented incrementally without inventing behavior.
- Created `docs/WEB.md` cataloguing every Thymeleaf page and shared
  fragment, and pinning the shared-service invariant.
- Created `CHANGELOG.md` (this file) and `INTERNAL_TODO.md`.
- Added a "Related repositories" section to `CLAUDE.md` pointing at the
  sibling mobile client `jacob-bensen/conexus-mail-mobile`.
- No production code changed. No new migrations. No API contract changes
  (there is no prior contract to break).
- Quality gates: `./mvnw test` **not run this session** — the routine
  reserved this Phase 0 run for doc bootstrap only, and no source files
  were modified. Next feature run will run the full suite.
