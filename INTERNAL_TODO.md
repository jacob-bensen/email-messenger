# Internal TODO (ordered by severity)

## Contract breakage
1. **No REST API for the mobile client.** `docs/API.md` has no `/api/v1` contract; auth is session-cookie/CSRF only. Design token auth + conversation list/messages/send endpoints in `docs/API.md` first, then implement over existing services. (Recommended next feature.)

## Logic drift
2. Prompt assumes jOOQ; backend uses JPA. Treat JPA as authoritative; no codegen step.
3. `CLAUDE.md` says Docker was removed (2026-06-11) but `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` exist. Reconcile (delete or update docs).
4. Scheduled-run prompt says commit to `master`; this session is pinned to branch `claude/gifted-carson-x5x9e8`. Work was pushed to the session branch, not master.

## Missing tests / docs
5. SSE event payload schema undocumented (`NewMailEvent`).
6. A tracked `master/` directory (BACKLOG.md, CHANGELOG.md, MASTER_ACTIONS.md) already holds a separate changelog/backlog; reconcile with root `CHANGELOG.md` / `INTERNAL_TODO.md`.

## Existing roadmap
Web Push, delivery states, signature contact cards, shared files, reply-to-bubble: see `CLAUDE.md` roadmap.
