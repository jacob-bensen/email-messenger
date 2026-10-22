# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No JSON `/api/v1` exists; mobile client has nothing to call. Next feature run: auth tokens + read-only
  chats list/detail (see `docs/API.md` §Planned).
- No bearer/token auth: only session cookie + CSRF. Needed before any mobile endpoint.
- `/chats/events` SSE payload is undocumented and session-bound.

## Logic drift
- Web logic lives in controllers (`ConversationController` builds list model, parses recipients,
  picks sending account/signature). Extract into services before an API reuses it, so web and API don't fork.
- `ReplyService` always threads onto the thread's last message (CLAUDE.md roadmap item).

## Missing / deferred
- Attachment bytes not stored (`blob_ref` null) — no download endpoint.
- Routine assumes jOOQ; repo uses JPA. Treat as JPA.
- Routine says commit to `master`; this session is pinned to feature branches by the harness and the repo
  CLAUDE.md forbids pushing to main. Commits go to the session branch; merging to master is a human step.
- Existing history files live in `master/` (BACKLOG, CHANGELOG, PLAN, MASTER_ACTIONS); root `CHANGELOG.md`
  started 2026-10-22 and supersedes `master/CHANGELOG.md` for this routine.
