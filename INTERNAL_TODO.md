# Internal TODO (sync audit 2026-10-24)

Ordered by severity.

## Contract breakage
- No mobile REST API or token auth exists; `docs/API.md` `/api/v1` is only
  proposed. Mobile client cannot talk to the server. **Next feature run:**
  bearer-token auth + `GET /api/v1/me` + conversation list, with matching Dart DTOs.
- CSRF + cookie sessions block native clients on `POST /chats/refresh` etc.

## Logic drift
- Chat logic lives partly in `ConversationController` (mailbox selection,
  send validation: body-or-attachment rule). Extract to a service before an API
  reuses it, to avoid forking the rule.
- Task prompt assumes jOOQ; backend uses JPA. Treat JPA as authoritative.

## Missing tests
- No tests for `/chats/refresh` JSON shape or SSE event format (contract-level).

## Deferred (from CLAUDE.md roadmap)
- Support email, Google OAuth consent publish, Web Push, delivery states,
  living contact card, shared files (attachment blobs), reply-to-bubble.

## Style / docs
- CLAUDE.md "Target package layout" is stale vs. actual packages (see docs/BACKEND.md).
