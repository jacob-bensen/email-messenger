# API.md — canonical API contract

## Status (2026-10-01): NO JSON API EXISTS YET
The server exposes only HTML (Thymeleaf), session-cookie auth + CSRF. The mobile client
cannot currently authenticate or fetch data. The only JSON-ish endpoints are
`POST /chats/refresh` → `{"imported": n}` (session+CSRF) and SSE `GET /chats/events`.

## Planned v1 (PROPOSAL — not implemented; implement before the client depends on it)
Base `/api/v1`, JSON, `Authorization: Bearer <token>`. Errors:
`{"error": {"code": "string", "message": "string"}}` with 400/401/403/404/409/413/429.
All handlers MUST delegate to the same services as the web pages.

| # | Method & path | Purpose | Backed by existing service |
|---|---|---|---|
| 1 | `POST /api/v1/auth/token` `{email,password}` → `{accessToken,expiresAt,refreshToken}` | login | `DbUserDetailsService` (TBD: token store) |
| 2 | `POST /api/v1/auth/refresh` | refresh | TBD |
| 3 | `GET /api/v1/conversations?q&filter&mailbox&page` → `{items:[ConversationListItem],counts}` | list | `ConversationListService.list/counts` |
| 4 | `GET /api/v1/conversations/{key}` → `ChatConversation` | open chat | `ChatService` |
| 5 | `POST /api/v1/conversations/{key}/messages` `{body,asReply,subject}` | send | needs extraction from `ConversationController.send` |
| 6 | `GET /api/v1/events` (SSE) | live | `ChatEventStream` |

DTO field names mirror the records `ConversationListItem`, `ChatConversation`, `BubbleRun`,
`BubbleMessage`, `ChatMember` (timestamps as UTC epoch millis, as `lastActivityEpochMilli`).
