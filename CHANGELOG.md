# CHANGELOG

Dated log of what the full-stack sync routine has changed in this repo.
Product changes shipped by hand or in earlier autonomous routines are in
`master/CHANGELOG.md`; this file starts at the first sync-routine run.

## 2026-08-28

- Bootstrapped the doc system required by the full-stack sync routine:
  added `docs/BACKEND.md` (architecture + package tour), `docs/API.md`
  (mobile-client contract; currently a stub because no REST endpoints
  exist yet), `docs/WEB.md` (Thymeleaf route inventory), `CHANGELOG.md`
  (this file), and `INTERNAL_TODO.md` (severity-ordered gap list).
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  the sibling mobile repo `conexus-mail-mobile`.
- Recorded the top-priority mismatch in `INTERNAL_TODO.md`: the mobile
  repo depends on a JSON REST API that does not yet exist on this
  backend, and neither does bearer-token auth.
- No code changed. Phase 0 of the sync routine consumed the run (docs
  + audit).
