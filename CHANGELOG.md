# CHANGELOG

Repo-level change log for the **email-messenger** backend, appended
per run of the full-stack sync routine. Product-strategy milestones
for the pre-launch conversion/churn work are tracked separately in
[`master/CHANGELOG.md`](master/CHANGELOG.md); this file is for
codebase- and contract-level changes visible to the mobile client and
future maintainers.

Entries are dated `YYYY-MM-DD` and appended top-first.

## 2026-08-28 — Phase 0 discovery & doc bootstrap

- Classified this repo as the **BACKEND** half of the two-repo product
  (paired with `jacob-bensen/conexus-mail-mobile`).
- Created the doc system required by the routine:
  - [`docs/BACKEND.md`](docs/BACKEND.md) — architecture, package
    layout, domain model, auth model, mailbox sync, billing, how
    Thymeleaf pages relate to services.
  - [`docs/API.md`](docs/API.md) — canonical JSON API contract. This
    is a **design document** — no JSON API exists in code yet; every
    endpoint is marked `Status: TBD` until it ships.
  - [`docs/WEB.md`](docs/WEB.md) — full page inventory for the
    Thymeleaf website, mapped to controllers, templates, and shared
    fragments.
  - [`INTERNAL_TODO.md`](INTERNAL_TODO.md) — sync-gap punch list, in
    severity order.
- Added a **Related repositories** section to `CLAUDE.md` pointing at
  the mobile repo and the doc set.
- Confirmed correction to the routine's assumed stack: this repo uses
  **Spring Data JPA**, not jOOQ. Docs describe the code as it is.

Nothing shipped that changes runtime behavior in this run. The single
biggest gap surfaced by Phase 0 is that the mobile client has **no
API to talk to** — the entire `/api/v1/*` surface designed in
`docs/API.md` still needs to be built, along with a token-based auth
path. That is now item #1 in `INTERNAL_TODO.md`.
