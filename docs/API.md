# API contract (canonical)

Status as of 2026-10-10: **no mobile REST API exists.** This file documents (1) the non-HTML endpoints that exist today and (2) the contract-first plan for `/api/v1`, which is **PLANNED, not implemented**. Client and server must not diverge from this file.

## 1. Existing non-HTML endpoints
All require the session cookie + CSRF header unless noted. Errors: unauthenticated → 302 to `/login` (not JSON).

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); one `NewMailEvent` per import for the user's mailboxes. Event payload shape: TBD (see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe event JSON | server-to-server only |
| GET | `/actuator/health` | public | — | Spring Boot health JSON |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml` | public | — | PWA / SEO assets |

## 2. PLANNED `/api/v1` (not implemented)
Design constraints (decide before implementing — TBD items need an owner decision):
- Auth: bearer token for mobile (TBD: opaque token table vs. JWT); `POST /api/v1/auth/login {email,password}`. Stateless, no CSRF. Must reuse `UserService`/`DbUserDetailsService` and the login throttle.
- Error body: `{"error": "<code>", "message": "<text>"}` with 400/401/403/404/409/413/429.
- Conversations list: `GET /api/v1/chats?mailbox=&q=&filter=&page=` backed by `ConversationListService`.
- Conversation: `GET /api/v1/chats/{key}` backed by `ChatService`; `POST /api/v1/chats/{key}/read`; `POST /api/v1/chats/{key}/messages` backed by `OutboundMessageService`/`ReplyService`.
- Live: reuse SSE `/chats/events` with bearer auth.
Concrete JSON schemas will be written here **before** any implementation, per the routine.
