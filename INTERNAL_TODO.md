# INTERNAL_TODO (cross-repo sync), ordered by severity

## Contract breakage
1. No mobile-capable API: no `/api/**`, no token auth (SecurityConfig is session+CSRF only). Mobile client cannot log in. Highest-value first feature: bearer-token auth + read-only `GET /api/v1/chats` and `/chats/{key}` (see docs/API.md §2).
2. `/chats/events` SSE payload shape undocumented (docs/API.md §1).

## Logic drift
3. Target stack mismatch: routine assumes jOOQ; backend uses JPA. Treat as intentional; don't add jOOQ.
4. `lib/domain/` rules to mirror (once client exists): conversationKey derivation (`ConversationKeyService`), plan limits (`PlanLimitKind`: THREAD_COUNT, MAILBOX_COUNT, SAVED_SEARCH_COUNT), quoted-reply stripping is server-side only (client should render server output, not re-derive).
5. Controllers contain list/scope logic (`ConversationController.scopeAddress/selectMailbox/populateList`) that an API would need — extract to a service before adding `/api/v1`.

## Missing tests
6. No test for `/chats/refresh` JSON shape or SSE endpoint contract.

## Deferred / style
7. README.md still says "MVP is being scaffolded" and mentions attachment download links that don't exist (`blob_ref` null).
8. CLAUDE.md roadmap items (Web Push, delivery states, …) unchanged.
