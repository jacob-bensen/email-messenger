# CHANGELOG — email-messenger (root, full-stack sync)

This is the sync-routine changelog that pairs with
`conexus-mail-mobile/CHANGELOG.md`. The backend's own per-ship
changelog lives at `master/CHANGELOG.md` and stays authoritative for
backend-only work.

One dated entry per full-stack sync run.

## 2026-08-29 — Phase 0 doc bootstrap

Scope: the full-stack sync routine ran for the first time in this
repo and found (a) no `docs/` folder, no root `CHANGELOG.md`, no
`INTERNAL_TODO.md`; (b) a mostly-empty sibling `conexus-mail-mobile`
repo (just a README); (c) no REST API endpoints intended for a mobile
client — every `web/*Controller` renders Thymeleaf, and the only
`@RestController` in the tree is the Stripe webhook.

Because Phase 0 required substantial work (docs created on both
sides, no contract to audit) this run is discovery + bootstrap only,
per the routine's guidance:

> "If Phase 0 required substantial work (docs created, mismatches
> found), you MAY spend this entire run on discovery + fixing the
> worst contract mismatches instead of a new feature."

### Backend (this repo)

- Added `docs/BACKEND.md` — architecture, package layout, domain
  model, auth model, live-delivery summary, index of authoritative
  docs.
- Added `docs/API.md` — canonical REST contract for the mobile
  client. Currently declares NO endpoints exist yet and lays out
  the design conventions (`/api/v1/*`, bearer-token auth, JSON
  camelCase, idempotency-key on mutations, error envelope) for
  future runs.
- Added `docs/WEB.md` — Thymeleaf page inventory grouped by
  controller, with the service-layer dependency table each page
  goes through. The rule "mobile endpoint must call the same
  service as its Thymeleaf sibling" is captured here.
- Added `INTERNAL_TODO.md` (root) — severity-ordered list of gaps;
  top blocker is "no REST API for the mobile client exists yet".
- Added `CHANGELOG.md` (this file) — sync-routine changelog. Does
  not replace `master/CHANGELOG.md` which the backend's own routine
  still writes.
- Updated `CLAUDE.md` with a "Related repositories" section pointing
  at `conexus-mail-mobile` and each doc's role.

### Mobile (`conexus-mail-mobile`)

Companion changes in that repo — see its own `CHANGELOG.md`.

### Not done this run

- No REST endpoints implemented.
- No Flutter code written beyond doc scaffolding.
- `./mvnw test` NOT run — the diff is docs-only, no Java touched.

### Follow-ups queued in `INTERNAL_TODO.md`

- Implement `POST /api/v1/auth/login` + `GET /api/v1/me` with a
  Flyway `refresh_tokens` migration.
- Extract a "verify Google ID token → provision user" method the
  mobile OAuth path can share with the web path.
