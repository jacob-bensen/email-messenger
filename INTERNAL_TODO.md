# Internal TODO (backend)

Ordered by severity.

## Contract breakage
- No mobile REST API exists; `docs/API.md` has only a proposal. Mobile repo is empty.
- No token (non-cookie) authentication for non-browser clients.

## Logic drift
- `ConversationController` holds orchestration (mailbox selection, list model assembly)
  that a REST API would need; extract into a service before adding `/api/v1/chats`.
- Target stack says jOOQ; repo uses JPA. Decision needed (keep JPA — recommended, matches CLAUDE.md).

## Missing tests / tooling
- Phase 0 did not run `mvn verify` (see CHANGELOG).
