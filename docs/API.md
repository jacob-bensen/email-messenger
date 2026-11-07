# API.md — canonical API contract

**Status (2026-11-07): there is no mobile REST API.** The server exposes only
Thymeleaf pages (`WEB.md`). This file is the contract both repos must follow;
it currently lists the only machine-readable endpoints that exist and marks
everything else TBD. No client may call anything not listed as **Implemented**.

## Implemented (session-cookie auth + CSRF, not mobile-suitable)
| Method/Path | Request | Response |
|---|---|---|
| `POST /chats/refresh` | none (CSRF token) | `200 {"imported": <int>}` |
| `GET /chats/events` | none | `text/event-stream` of new-mail events (`ChatEventStream`) |
| `POST /billing/webhook` | Stripe-signed payload | Stripe-only; not for clients |

## Planned (TBD — design before implementing)
- `POST /api/v1/auth/token` (bearer/refresh tokens; stateless, no CSRF)
- `GET /api/v1/chats?q&mailbox&filter&page` → conversation list
- `GET /api/v1/chats/{key}` → timeline (bubble runs)
- `POST /api/v1/chats/{key}/send` → send (multipart for attachments)
- `POST /api/v1/chats/{key}/read`
- `GET /api/v1/mailboxes`
- Error envelope: `{"error": {"code": string, "message": string}}` (proposed)
Each must be backed by the same services the web controllers use
(`ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`).
