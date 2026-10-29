# API contract

**Status: no mobile REST API is implemented.** The server only exposes HTML pages plus the machine endpoints below.
The mobile client (`conexus-mail-mobile`) currently has no code, so there is no client/server drift yet.
New mobile endpoints MUST be specified here *before* implementation (contract-first), under `/api/v1/**`, using the same service layer as the web controllers.

## Existing non-HTML endpoints
| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` – triggers an immediate poll of the user's active mailboxes | 302 to /login if unauthenticated; 403 on bad CSRF |
| GET | `/chats/events` | session | none | `text/event-stream`; events published from `NewMailEvent` (payload schema TBD – read `ChatEventStream`) | 302 /login |
| POST | `/billing/webhook` | Stripe signature header, CSRF exempt | raw Stripe event JSON | 200 | 400 invalid signature |
| GET | `/actuator/health` | none | – | Spring Actuator health | – |

## Planned (TBD, not implemented)
Token auth (login/refresh), `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`, mailbox list, plan/limits. Schemas TBD.
