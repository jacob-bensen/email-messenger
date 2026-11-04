# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. **No REST API exists** for the mobile client (no `/api/v1`, no bearer auth;
   web uses session+CSRF). Define in `docs/API.md`, implement over existing
   services, add stateless security filter chain for `/api/**`.
2. Prompt assumes jOOQ; backend uses JPA. Treat JPA as authoritative (no change planned).

## Logic drift
- None found (single web client). Keep new API controllers thin over services.
- `ConversationController` builds model attributes inline (billing banner, mailbox scoping) — extract scoping helpers (`selectMailbox`, `scopeAddress`) into a service so `/api/v1` can reuse them.

## Missing tests
- No controller tests for the chat JSON endpoint `POST /chats/refresh`.

## Roadmap items (see CLAUDE.md): Web Push, delivery states, attachments blob store, reply-to-bubble.
