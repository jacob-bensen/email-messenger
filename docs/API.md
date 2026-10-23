# API CONTRACT (canonical)

Status: **No mobile REST API exists yet.** The only JSON endpoint is `POST /chats/refresh`. This file records the real current surface and the (TBD) planned `/api/v1` contract. Nothing under "Planned" is implemented; the client must not call it until it moves to "Implemented".

## Implemented (cookie session + CSRF)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | 200 `{"imported": <int>}` |
| GET | `/chats/events` | session | – | `text/event-stream`, `NewMailEvent` |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | Stripe event JSON | 200 |
| GET | `/actuator/health` | none | – | Spring health |
All other routes return HTML (see WEB.md). Errors: `GlobalExceptionHandler` maps IllegalArgumentException→400; unauthenticated → 302 `/login`.

## Planned `/api/v1` (TBD — design before building)
Needs: bearer-token auth (login → access/refresh token) instead of cookie+CSRF; `GET /api/v1/conversations?mailbox=&cursor=`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`, `POST /api/v1/mailboxes/refresh`; JSON error envelope `{code,message}`. Backed by `ConversationListService` / `ChatService` / `OutboundMessageService`.
