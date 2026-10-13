# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No `/api/**` REST API exists, so the mobile client (`conexus-mail-mobile`) has nothing to call.
  Needs: bearer-token auth (login endpoint + filter), then read endpoints (me, chats, chat detail,
  mailboxes), then send. Define in `docs/API.md` first.
- No token auth: session cookie + CSRF only. Mobile needs a stateless chain scoped to `/api/**`.
- `/chats/events` SSE payload shape undocumented.

## Logic drift / architecture
- `ConversationController` holds send/reply orchestration (asReply branching, signature,
  attachment mapping, mailbox scoping). Extract into a service so a REST controller shares it.
- Stack deviates from the routine's assumed stack: JPA (no jOOQ). Do not add jOOQ; docs say so.

## Repo hygiene
- Stray tracked directory `master/` (BACKLOG.md, 500 KB CHANGELOG.md, PLAN.md, MASTER_ACTIONS.md)
  from an old merge. Root `CHANGELOG.md` now exists; decide whether to delete/merge `master/`.
- `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` exist although CLAUDE.md says Docker was removed.
- Branching: routine prompt says `master`, but this session was pinned to
  `claude/gifted-carson-a2r4wx` by the environment; work landed there.

## Missing tests / verification
- `mvn compile` could not be confirmed in the 2026-10-13 run (see CHANGELOG for result).
