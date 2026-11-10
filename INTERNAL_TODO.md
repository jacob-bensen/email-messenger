# INTERNAL_TODO (severity-ordered)

## Contract breakage
- No `/api/**` exists; `docs/API.md` only proposes v1. Mobile client has nothing to call.
- No token (non-cookie) auth for mobile; session+CSRF only.

## Logic drift
- Routine assumes jOOQ; repo uses JPA. Treat JPA as authoritative (record in docs).
- Routine says commit to `master`; this environment mandates the session branch `claude/gifted-carson-okid8j` — followed that.

## Missing / TBD
- SSE `/chats/events` payload schema undocumented.
- Attachment bytes not stored (`blob_ref` null) — blocks mobile downloads.
- Not run this session: `mvn verify` (docs-only change).

## Suggested first feature (needs both repos)
Token auth + `GET /api/v1/chats` list, backed by `ChatService`/`ConversationService`.
