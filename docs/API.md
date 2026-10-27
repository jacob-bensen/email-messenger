# API contract

**Status (2026-10-27): no mobile/JSON API exists.** This file documents the
JSON/stream endpoints that actually exist, then the *proposed* v1 mobile API
(marked PROPOSED — not implemented; neither repo may rely on it until a run
implements it and flips the status here).

## Existing machine-facing endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; one event per `NewMailEvent` (payload shape TBD — see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | Stripe event JSON | 200; invalid signature → `InvalidStripeSignatureException` (status mapping TBD) |
| GET | `/actuator/health` | none | — | Spring Boot health |

Errors elsewhere are HTML via `GlobalExceptionHandler` (`error.html`); 400 for
`IllegalArgumentException`, 404 for missing resources, 413 for oversized uploads.

## PROPOSED v1 mobile API (not implemented)
Base path `/api/v1`, JSON, bearer token auth. Server authoritative; reuse
`ChatService`/`ConversationListService`/`ReplyService`.
- `POST /api/v1/auth/token` `{email,password}` → `{accessToken,expiresAt}` (TBD: refresh, Google sign-in)
- `GET /api/v1/chats` → conversation list (`key`, `title`, `lastMessageAt`, `unread`, `preview`)
- `GET /api/v1/chats/{key}` → bubbles
- `POST /api/v1/chats/{key}/send` `{body}` → created bubble
- Error body: `{"error": "<code>", "message": "<text>"}` (TBD codes)
Exact schemas TBD; finalize here *before* coding.
