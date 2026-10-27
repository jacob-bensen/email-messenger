# Internal TODO

## Contract breakage
- No mobile REST API or token auth exists; client repo is empty. Define `/api/v1` in `docs/API.md` first,
  then build it on `ConversationService`/`ChatService`/`ReplyService`.

## Logic drift
- Business logic lives partly in controllers (`ConversationController` is large) — extract to services
  before exposing to a REST layer to avoid web/API drift.
- Routine prompt assumes jOOQ; backend uses JPA. Routine says commit to `master`; this session was
  restricted to branch `claude/gifted-carson-1jhcxo` (and CLAUDE.md forbids pushing to main).

## Missing tests / verification
- `NewMailEvent` SSE payload undocumented; IDLE unverified on real IMAP (see CLAUDE.md roadmap).

## Style
- `master/` directory (PLAN/BACKLOG/CHANGELOG, ~500KB changelog) is the earlier routine's state; keep, don't edit here.
