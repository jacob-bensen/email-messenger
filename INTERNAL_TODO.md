# Internal TODO (ordered by severity)

## Contract breakage
- No mobile REST API exists; `conexus-mail-mobile` has no code. Implement `/api/v1` (bearer-token auth + conversations list/detail/send) per `docs/API.md` PLANNED section, backed by the existing services (`ConversationListService`, `ChatService`, `OutboundMessageService`).
- `/chats/events` SSE payload shape undocumented (TBD in API.md).

## Logic drift
- Target-stack mismatch: task template assumes jOOQ; repo uses JPA. Decision: keep JPA, do not migrate.
- Business rules the mobile `lib/domain/` must mirror: conversationKey (`ConversationKeyService`), quoted-reply stripping (`EmailBodyCleaner`), bubble grouping (`IMTransformService`), plan limits (`PlanLimits`).

## Missing tests / verification
- (none known beyond the above; baseline `mvn -q -B test` passed 2026-10-13)

## Deferred (from CLAUDE.md roadmap)
Support email, Google OAuth consent publish, Web Push, delivery states, signature contact cards, attachment storage, reply-to-bubble.
