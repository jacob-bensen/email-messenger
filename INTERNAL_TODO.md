# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No mobile REST API exists; `docs/API.md` has only a proposal. Mobile repo cannot integrate until `/api/v1` + token auth are specified and built.
- Spring Security is session+CSRF only; no stateless `/api/**` filter chain.

## Logic drift
- None possible yet (single web layer). When `/api/v1` is added, route through existing services only.

## Missing tests / gaps
- Attachments are not downloadable (`blob_ref` null) — blocks mobile attachment support (see CLAUDE.md roadmap).
- IMAP IDLE unverified against a real server (CLAUDE.md roadmap).

## Process
- Routine prompt says "master branch / jOOQ"; repo is JPA and cloud sessions are pinned to `claude/*` branches. Reconcile with the owner. Also, `master/` is a folder of planning docs (PLAN/BACKLOG/CHANGELOG) in this repo, separate from root `CHANGELOG.md`.
