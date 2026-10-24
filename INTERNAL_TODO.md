# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No `/api/v1` REST API exists; mobile client has nothing to call. Needs token auth in `SecurityConfig` (stateless filter chain for `/api/**`, CSRF-exempt) and DTOs.
- Attachments have no stored bytes (`blob_ref` null) → mobile can't download them.
## Logic drift / architecture
- `web` services (`ChatService`, `ConversationListService`, `OutboundMessageService`) are package-private; an API package must reuse them → move to `service` or widen.
- `ConversationController.send` contains orchestration (mailbox selection, signature, attachments) that should move into a service before an API duplicates it.
- Stack differs from the routine's assumption (JPA, not jOOQ).
## Missing tests / verification
- `mvn -q -B verify` passes as of 2026-10-24; no tests exist for a JSON API (none exists).
