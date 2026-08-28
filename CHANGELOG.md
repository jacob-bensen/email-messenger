# CHANGELOG — email-messenger (backend)

Dated entries per full-stack-sync run: what changed and why. Older
per-epic history lives in `master/CHANGELOG.md`.

## 2026-08-28 — Phase 0 bootstrap + `GET /api/v1/version`

- Bootstrapped the doc system required by the full-stack sync routine:
  `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, plus this
  `CHANGELOG.md` and `INTERNAL_TODO.md`. Documented the current
  Thymeleaf-only surface, the auth model, and the plan for a
  bearer-token REST API for the sibling mobile client.
- Added the first REST controller, `com.emailmessenger.web.api.VersionController`,
  serving `GET /api/v1/version` with a small JSON payload
  (`app`, `apiVersion`, `buildVersion`, `serverTime`). No auth
  required — lets the sibling `conexus-mail-mobile` client prove
  connectivity and negotiate future capability flags. Whitelisted the
  path in `SecurityConfig`.
- Recorded the sibling repo (`conexus-mail-mobile`) and its docs in
  `CLAUDE.md`'s "Related repositories" section.
- Filed the outstanding contract work (mobile auth, chat endpoints, SSE
  payload spec) in `INTERNAL_TODO.md`, ordered by severity.
