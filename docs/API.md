# API contract (canonical)

**Status: no mobile REST API is implemented.** Nothing under `/api/**` exists and there is no token auth.
The mobile repo (`conexus-mail-mobile`) is an empty scaffold, so there is no client/server drift yet.
Everything below is what exists today; the planned v1 surface is marked TBD.

## Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | SSE stream of new-mail events (`NewMailEvent` via `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe-Signature header, no CSRF | Stripe event JSON | `200`; `400` missing/invalid signature; `503` not configured |

Errors: HTML error page via `GlobalExceptionHandler`/`error.html`; no JSON error envelope defined (TBD).

## Planned v1 (TBD — to be defined before implementation)
Token auth (login → access/refresh), `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`,
`POST /api/v1/conversations/{key}/messages`, `GET /api/v1/mailboxes`, push-token registration. Must reuse
`ChatService`/`ConversationService`/`ReplyService` — no logic in controllers.
