# CHANGELOG — email-messenger

Dated entries appended per routine run.

For product/feature history, see `master/CHANGELOG.md` (older,
epic-scoped). This file tracks routine-scoped changes to the codebase
and the cross-repo contract.

## 2026-08-30

- Bootstrapped the cross-repo doc system per the full-stack sync routine:
  - `docs/BACKEND.md` — module layout, domain model, auth model,
    Flyway inventory, live-delivery pipeline, build/run commands.
  - `docs/API.md` — enumerated the four machine-readable endpoints that
    exist today (SSE stream, JSON refresh heartbeat, Stripe webhook,
    PWA/SEO assets) and sketched the proposed `/api/v1/**` surface for
    the future Flutter client; flagged the sketch as NOT YET IMPLEMENTED.
  - `docs/WEB.md` — full inventory of Thymeleaf pages, controllers,
    fragments, and shared layout rules.
  - `INTERNAL_TODO.md` — captured the biggest cross-repo gap: no
    mobile-facing REST API and no non-cookie auth path.
  - `CHANGELOG.md` — this file.
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  the sibling `conexus-mail-mobile` repo and the new doc tree.
- No code, migration, or template changes; no roadmap boxes ticked. The
  routine's Phase 0 accommodation explicitly allows spending a run on
  discovery + doc scaffolding when substantial work is needed to get
  the doc system in place.
