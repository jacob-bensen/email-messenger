# API contract

**Status (2026-10-05): no mobile REST API is implemented.** This file is the canonical contract; it separates what exists
from what is planned. Nothing under "Planned" may be called by the client until implemented and moved to "Implemented".

## Implemented (JSON / non-HTML)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | SSE stream of new-mail events (payload shape TBD — read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe webhook; not for clients |
| GET | `/actuator/health` | public | Spring health |

All other routes return HTML (see `docs/WEB.md`). Auth is cookie-session + CSRF; errors render `error.html`.

## Planned (TBD — design only, not implemented)
Prefix `/api/v1`, JSON, `Authorization: Bearer <access token>`; errors `{ "error": code, "message": text }` with
401/403/404/422/429. Endpoints to add (each must call the same services as the web controllers):
- `POST /api/v1/auth/login`, `/auth/refresh`, `/auth/logout` — needs token model (TBD: JWT vs opaque, device table).
- `GET /api/v1/chats?mailbox&filter&q&page` — wraps `ConversationListService.list/counts` (item = `ConversationListItem`).
- `GET /api/v1/chats/{key}` — wraps `ChatService` (`ChatConversation`: key,title,initials,members[],threadCount,runs[]); marks read.
- `POST /api/v1/chats/{key}/send` — body `{body, subject?, asReply?, mailboxId?}`; attachments multipart.
- `GET /api/v1/mailboxes`.
Prerequisite: extract the send logic from `ConversationController.send` into a service (see INTERNAL_TODO).
