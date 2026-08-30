# CHANGELOG — backend (`email-messenger`)

Append a dated entry per full-stack-sync run. Each entry lists the
feature chosen, what shipped in this repo, contract changes, and
anything deferred to `INTERNAL_TODO.md`.

## 2026-08-30 — Phase 0 bootstrap

- Classified this repo as the BACKEND for the two-repo sync routine;
  sibling repo is `jacob-bensen/conexus-mail-mobile` (mobile client, a
  fresh Flutter scaffold on this run).
- Bootstrapped the doc system per the sync routine's Phase 0:
  - Added `docs/BACKEND.md` (architecture, domain model, auth model).
  - Added `docs/API.md` — canonical wire contract; today an empty
    catalogue with conventions and a status note that no mobile-facing
    endpoints exist yet.
  - Added `docs/WEB.md` — Thymeleaf page inventory and the website-vs-
    mobile rule (share the service layer).
  - Added this `CHANGELOG.md` and `INTERNAL_TODO.md`.
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  the mobile repo and each doc's role.
- No code changes on this run; no schema migration; no test changes.
- Contract changes: none (contract established at v0 with an empty
  endpoint catalogue).
- Deferred: everything under Phase 1 — the first `/api/v1` endpoints,
  the auth-for-mobile decision, and the first end-to-end feature. See
  `INTERNAL_TODO.md`.
