# INTERNAL_TODO.md (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] **No mobile API / token auth.** The mobile client has nothing to call.
      Implement `/api/v1/auth/*`, `/api/v1/me`, `/api/v1/conversations*` per
      `docs/API.md` §2 (new Flyway migration for refresh tokens; reuse
      `ChatService` etc.). Recommended first feature run: login + `me` +
      conversation list, with a Flutter login screen and list view.
- [ ] `GET /chats/events` SSE event name/payload undocumented (TBD in API.md).

## Logic drift
- [ ] Web/REST duplication audit: N/A until REST exists; `POST /chats/refresh`
      is the one JSON endpoint and uses `MailboxPollingService` (shared). OK.

## Docs / stack mismatches
- [ ] Generic routine assumes jOOQ; repo uses JPA. Decide whether to keep JPA
      (recommended) and drop jOOQ from the routine.
- [ ] Roadmap in `CLAUDE.md` says Docker and GitHub Actions CI were removed
      (2026-06-11), but `Dockerfile`, `docker-compose.yml`, `.dockerignore` and
      `.github/` exist in the tree. Reconcile.
- [ ] `master/` directory holds PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS from an
      earlier routine; root `CHANGELOG.md` now exists for this routine. Decide
      whether to merge them.
- [ ] Branching: routine says commit to `master`; this session's environment
      requires the `claude/*` feature branch. Resolve which wins.

## Missing tests
- [ ] none identified yet beyond those above (see test run in CHANGELOG).
