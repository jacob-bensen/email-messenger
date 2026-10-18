# API contract (canonical)

**Status: no mobile REST API exists yet.** This file documents what is actually exposed today and a clearly-marked *proposed* v1 for the Flutter client. Anything under "Proposed" is TBD and not implemented on either side.

## Current machine-consumable endpoints
Auth for all: session cookie; state-changing requests need the CSRF token (`_csrf` form field or `X-CSRF-TOKEN`).

| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | none | `200 {"imported": <int>}` — new messages imported synchronously for the user's mailboxes | 302→`/login` if unauthenticated; 403 CSRF |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); events published from `NewMailEvent` via `ChatEventStream` (event names/payload: TBD, read `ChatEventStream`) | 302→`/login` |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 200 | invalid signature → rejected (`InvalidStripeSignatureException`) |
| GET | `/actuator/health` | public | — | Spring health JSON | — |

All other routes return HTML or redirects (see `WEB.md`); errors render `error.html`.

## Proposed v1 (TBD — to be designed before implementation)
Needs, in order: (1) token auth (`POST /api/v1/auth/login` → bearer token; reuse `UserService`/`LoginThrottleService`), (2) `GET /api/v1/chats` (reuse `ConversationListService`), (3) `GET /api/v1/chats/{key}` (reuse `ChatService`/`ChatConversation`), (4) `POST /api/v1/chats/{key}/send` (reuse `OutboundMessageService`), (5) push/SSE. Each must go through the existing service layer; CSRF is not applicable to bearer-token routes. Shapes are deliberately not specified until a feature run implements them.
