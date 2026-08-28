# Changelog

All notable changes to the `email-messenger` backend/website. This log
is appended to on every sync-routine run. Existing history predating
the routine is not backfilled here — see `git log` for that.

## 2026-08-28 — Sync routine bootstrap (Phase 0)

- Added `docs/BACKEND.md` — architecture overview: runtime, packages,
  domain, auth, email import, IM transform, live delivery, billing,
  admin, and how the Thymeleaf site and mobile REST API share the same
  service layer.
- Added `docs/API.md` — canonical REST API contract for the
  `conexus-mail-mobile` Flutter client. Records the target
  authentication model (short-lived bearer + refresh, distinct from the
  web app's session cookies) and the initial endpoint shapes for
  `auth`, `me`, `mailboxes`, `chats`, `saved-searches`, and `health`.
  No endpoints implemented yet.
- Added `docs/WEB.md` — page-by-page inventory of Thymeleaf routes,
  templates, and controllers; documents the invariant that pages and
  REST endpoints share the service layer.
- Added `INTERNAL_TODO.md` — running list of contract mismatches, tech
  debt, and deferred work discovered by the sync routine. Seeded with
  the biggest gap: no mobile REST API exists yet.
- Updated `CLAUDE.md` with a **Related repositories** section pointing
  at `conexus-mail-mobile` and each doc's role.

Nothing shipped in production code this run; scope was intentionally
limited to the doc system + repo linkage. The mobile client is being
scaffolded from empty in the sibling repo at the same time.
