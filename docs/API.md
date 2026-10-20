# API contract (canonical)

Status: **no mobile REST API is implemented.** This file records what exists and what is TBD. Any new endpoint MUST be added here first, then implemented on both sides.

## Existing machine-facing endpoints (session-cookie auth, CSRF token required for POST)
| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session | none | 200 `{"imported": <int>}` | 302→/login if unauthenticated; 403 on missing CSRF |
| GET | `/chats/events` | session | none | `text/event-stream` (new-mail events; payload shape TBD — see `NewMailEvent`/`ChatEventStream`) | 302→/login |
| POST | `/billing/webhook` | Stripe signature header | Stripe event JSON | 200 | 400 invalid signature |

## Everything else
All other routes return HTML (see `WEB.md`). They are not part of the client contract.

## Planned (TBD, not implemented)
- Token auth for mobile (`POST /api/v1/auth/login` → bearer token) — format TBD.
- `GET /api/v1/chats`, `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send` returning JSON built from the same `ChatService` / `ConversationListService` / `OutboundMessageService` the web pages use.
- Error envelope: TBD.
