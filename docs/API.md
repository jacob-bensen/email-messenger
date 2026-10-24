# API — canonical contract for the mobile client

> **Status 2026-10-24: NO JSON/REST API EXISTS.** The server exposes only
> session-cookie + CSRF Thymeleaf routes (`WEB.md`) and one JSON-ish endpoint
> (`POST /chats/refresh` → `{"imported": <int>}`), plus the SSE stream
> `GET /chats/events`. The mobile client cannot talk to the server yet.

## Planned v1 (PROPOSED, TBD — not implemented, not agreed)
Base path `/api/v1`, JSON, stateless bearer auth (access + refresh token),
errors as `{ "error": { "code": "...", "message": "..." } }`.

Candidate first slice (read-only, maps onto existing `ChatService`/`ConversationListService`):
- `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}`
- `GET  /api/v1/conversations?filter=&mailbox=&page=` → list items
- `GET  /api/v1/conversations/{key}` → bubble runs (mirrors `ChatConversation` record)
- `POST /api/v1/conversations/{key}/messages` → send (mirrors `/chats/{key}/send`)

Open decisions: token mechanism vs. existing session security; DTO shape
(reuse records in `service/` vs. dedicated API DTOs); attachments (no bytes stored today).
