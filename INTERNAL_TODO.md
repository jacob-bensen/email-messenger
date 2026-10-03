# INTERNAL_TODO

Ordered by severity.

## Contract breakage
- No REST API / token auth exists; mobile client cannot talk to the server. First
  feature: `/api/v1/auth/login` + read-only chat list (see docs/API.md "Planned").
- SSE event name/payload for `/chats/events` undocumented (docs/API.md TBD).

## Logic drift
- None detectable yet (no second consumer). Keep new API on `ConversationListService`/`ChatService`/`OutboundMessageService`.

## Missing tests / gates
- `mvn test-compile` could not be confirmed in the 2026-10-03 routine run (see CHANGELOG).
- Roadmap items in CLAUDE.md (support email, OAuth publish, Web Push, delivery states, …) remain open.

## Style / process
- Routine prompt assumes jOOQ and `master`; repo uses JPA and session branches — routine prompt should be updated.
