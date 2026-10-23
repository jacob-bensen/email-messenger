# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; mobile repo has no code. First feature run must define the
  contract in `docs/API.md` (auth scheme, error envelope, conversation list/detail/send),
  then implement controllers over `ConversationListService`/`ChatService`/`OutboundMessageService`.
- Auth is cookie+CSRF only; mobile needs token auth (decision pending).

## Logic drift
- `ConversationController` holds mailbox-scope logic (`selectMailbox`, `scopeAddress`,
  `chatFor`) — extract into a service before a REST layer reuses it, to avoid forking.
- Stack mismatch vs. generic routine: JPA, not jOOQ.

## Missing tests / docs
- SSE event payload shape undocumented (`NewMailEvent`).
- `mvn` verify not run in this session's Phase 0 (see CHANGELOG).

## Housekeeping
- `master/` directory holds legacy routine files (PLAN/BACKLOG/MASTER_ACTIONS and a 500KB CHANGELOG); root `CHANGELOG.md` is the routine's log going forward.
