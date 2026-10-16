# API contract

**Status (2026-10-16): no mobile/REST API exists yet.** This file is the canonical contract; it
currently lists only what the server really exposes in non-HTML form. Planned `/api/v1` surface is
under "Proposed" and is NOT implemented — the client must not code against it until it moves out of
"Proposed".

## Conventions (current)
- Auth: session cookie + CSRF token (browser only). Unauthenticated -> 302 `/login`.
- Errors: HTML error page via `GlobalExceptionHandler` (404 `NoSuchElementException`, 502 mail errors, etc.). No JSON error envelope yet.

## Implemented non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | none | `200 {"imported": <int>}` — synchronous poll of the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; event `new-mail` on import (payload TBD — see `ChatEventStream`); 30 min emitter timeout |
| POST | `/billing/webhook` | Stripe-Signature header | raw Stripe JSON | `200 ok` / `400` missing or invalid signature / `503` not configured / `500` apply failure |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/robots.txt`, `/sitemap.xml`, icons | public | | PWA/SEO |
| GET | `/actuator/health` | public | | Spring health |

All other routes are HTML form/page routes — see `docs/WEB.md`.

## Proposed `/api/v1` (NOT implemented; design only)
Prereqs: token auth (bearer, issued by `POST /api/v1/auth/token`, CSRF-exempt), JSON error envelope
`{"error":{"code","message"}}`, and extraction of controller-private chat logic into services.
- `POST /api/v1/auth/token`, `DELETE /api/v1/auth/token`
- `GET /api/v1/chats?mailbox&q&filter&page` — conversation list (`ConversationListService`)
- `GET /api/v1/chats/{key}` — bubbles (`ChatService`/`ChatConversation`), marks read
- `POST /api/v1/chats/{key}/messages` — send (`OutboundMessageService`/`ReplyService`)
- `GET /api/v1/mailboxes`, `GET /api/v1/me` (plan + limits)
- Live updates: reuse SSE `/chats/events` with bearer auth, or FCM push (TBD).
