# INTERNAL_TODO (severity-ordered)

## Contract breakage
- No REST API / token auth exists; mobile client cannot talk to the server.
  Define `/api/v1` in `docs/API.md`, add bearer auth (stateless filter chain
  alongside the session chain), reuse `ChatService`/`OutboundMessageService`.

## Logic drift
- None detected between web and (nonexistent) API. Watch that
  `ConversationController` keeps mailbox-scoping logic (`selectMailbox`,
  `scopeAddress`) in the controller; move into a service before an API needs it.

## Missing tests / verification
- `./mvnw test` was not run in the Phase 0 run (docs-only changes).

## Style / process
- Stack deviates from the routine's generic target (JPA, not jOOQ). Accepted.
- Routine says commit to `master`; this session is pinned to feature branch
  `claude/gifted-carson-0gbist`, and repo CLAUDE.md forbids pushing to main.
- Legacy planning docs live in `master/` (PLAN, BACKLOG, CHANGELOG ~500KB);
  root `CHANGELOG.md` is the routine's log.
