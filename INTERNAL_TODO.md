# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API or token auth exists; `docs/API.md` has `/api/v1` as TBD.
  The mobile repo (`conexus-mail-mobile`) cannot talk to the server yet.
  Next feature run: token auth (login → bearer) + read-only conversation list
  and conversation endpoints reusing `ConversationListService`/`ChatService`.
- `GET /chats/events` payload schema undocumented (read `ChatEventStream`).

## Logic drift
- Controllers (`ConversationController`) assemble view models themselves
  (`populateList`, `selectMailbox`, `scopeAddress`); extract into a service
  before an API reuses it, to avoid duplicating that logic.
- Routine template assumes jOOQ; repo uses JPA. Keep JPA.

## Missing tests
- Not audited this run (no build executed).

## Style / process
- Branching: the routine prompt says commit to `master`, but the session is
  pinned to `claude/gifted-carson-xhw2yz` / `claude/trusting-cori-xhw2yz` and
  repo CLAUDE.md forbids pushing to main; work goes to those branches.
- Pre-existing notes live in `master/` (BACKLOG, PLAN, MASTER_ACTIONS,
  CHANGELOG 500KB). Root `CHANGELOG.md` is new; older history is in
  `master/CHANGELOG.md`.
