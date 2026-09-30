# INTERNAL_TODO

## Contract breakage
- No REST `/api/v1` or token auth for the mobile client (chain is session + CSRF). Design + implement first slice:
  login → list chats → open chat → send → mark read, reusing `ChatService`/`ConversationListService`/`OutboundMessageService`.

## Logic drift
- Controllers (`ConversationController`) assemble model/filter parsing (`parseFilter`, `selectMailbox`, `scopeAddress`)
  inline; extract into a service before a REST controller needs the same logic.

## Missing tests / docs
- `docs/API.md`: Stripe webhook error codes and SSE payload schema are TBD.
- Generic routine assumes jOOQ; this repo uses JPA — keep it that way unless the owner says otherwise.
- Branching: routine prompt says `master`, but this session was pinned to feature branches; reconcile.
