# Changelog (backend)

Dated entries per full-stack sync run.

## 2026-08-31

- Bootstrapped the doc system for the two-repo full-stack workflow:
  `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, this changelog, and
  `INTERNAL_TODO.md`.
- Audited the codebase against a REST-API contract for the mobile client.
  Finding recorded in `INTERNAL_TODO.md`: **there is no `/api/v1` yet** —
  every controller today is Thymeleaf-only (Stripe webhook is the sole
  `@RestController`). `docs/API.md` documents the planned v1 surface so
  the mobile client can be built against it in parallel.
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  `conexus-mail-mobile` and the new docs.
