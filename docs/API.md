# API contract (canonical; shared with `conexus-mail-mobile`)

## Status: NO mobile REST API IS IMPLEMENTED
The server exposes only browser (HTML/form, cookie + CSRF) endpoints; see `docs/WEB.md`. No `/api/**` routes, no bearer tokens, no JSON DTOs for chats.
The mobile repo therefore calls nothing yet. Contract entries below are **PLANNED (not implemented)** and become binding only once implemented in the same commit.

## Non-HTML endpoints that exist today
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe events; not for clients |
| POST | `/chats/refresh` | session + CSRF | `@ResponseBody`, browser use (TBD: response schema) |
| GET | `/chats/events` | session | SSE stream of new-mail events (`NewMailEvent`) |
| GET | `/actuator/health` | public | health |

## PLANNED v1 (first feature, not yet implemented)
Conventions: base `/api/v1`, JSON, `Authorization: Bearer <token>`, errors `{ "error": "<code>", "message": "<text>" }` with 400/401/403/404/409/429.
- `POST /api/v1/auth/token` `{email,password}` → `{accessToken, expiresAt}` (TBD: refresh strategy; Google sign-in on mobile TBD)
- `GET /api/v1/chats?cursor=` → `{items:[ConversationListItem], nextCursor}`; item fields mirror `service/ConversationListItem`: `key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail`
- `GET /api/v1/chats/{key}` → conversation with bubble runs (mirror `BubbleMessage`: `messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId`)
- `POST /api/v1/chats/{key}/send`, `POST /api/v1/chats/{key}/read` — via existing `ReplyService` / `ChatService.markRead`
All must delegate to the same services the Thymeleaf controllers use.
