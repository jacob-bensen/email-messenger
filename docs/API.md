# API.md — canonical API contract (client ↔ server)

**Status as of 2026-10-28: NO MOBILE REST API IS IMPLEMENTED.** The server only exposes session-cookie, CSRF-protected, server-rendered endpoints (see `WEB.md`). This file is the place the contract will live; both repos must match it. Nothing below "Implemented" may be assumed to exist.

## Implemented (non-HTML) endpoints
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| `GET /chats/events` | session | none | `text/event-stream`; emits new-mail events (payload shape TBD — see `NewMailEvent`/`ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header | Stripe event JSON | `200` / `400` on bad signature |
| `GET /actuator/health` | public | none | Spring Boot health JSON |

Errors: no JSON error envelope exists (`GlobalExceptionHandler` renders `error.html`). TBD.

## Planned v1 (DRAFT — not implemented, not binding until a feature run implements it)
Existing server view models that the first API should serialize 1:1 (all in `com.emailmessenger.service`):
- `ConversationListItem {key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts[],senderEmail}`
- `ChatConversation {key,title,initials,members[ChatMember],threadCount,runs[SenderBubbleRun]}` → `BubbleRun/BubbleMessage {messageId,bodyHtml,sentAt,attachments,outbound,replyToMessageId}`
- Filters: `ALL|UNREAD|AWAITING|ATTACHMENTS`; page size 30.

Proposed endpoint set (first slice): `POST /api/v1/auth/token`, `POST /api/v1/auth/refresh`, `GET /api/v1/chats`, `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`, `GET /api/v1/mailboxes`. Auth: bearer token in a separate stateless `SecurityFilterChain` (CSRF off for `/api/**`). Error envelope proposal: `{"error":{"code":"...","message":"..."}}`. Details to be fixed in the feature run that implements them.
