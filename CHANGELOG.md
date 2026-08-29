# Changelog — email-messenger

## 2026-08-29 — Phase 0 bootstrap

- Added `docs/BACKEND.md` — architecture overview (modules, persistence,
  auth, chat model, live delivery, scheduling, testing, build) reverse-
  engineered from the current source.
- Added `docs/API.md` — canonical mobile API contract at `/api/v1/**`.
  No endpoints implemented yet; the file exists so
  `conexus-mail-mobile` can build to the same shapes.
- Added `docs/WEB.md` — Thymeleaf route inventory grouped by area.
- Added `INTERNAL_TODO.md` — severity-ordered list of the gaps found in
  the sync audit, starting with "no REST API exists yet".
- Updated `CLAUDE.md` with a **Related repositories** section pointing at
  the mobile client and each doc's role.

No production behavior changed. No migrations added. `./mvnw test` was
not run this iteration (docs-only change; see summary).
