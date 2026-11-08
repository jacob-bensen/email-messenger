# API contract (canonical, shared with `conexus-mail-mobile`)

**Status: no mobile REST API exists yet.** This file is the contract of record; the
mobile client must not call anything not listed under "Defined" below. Entries under
"Existing non-HTML endpoints" are real but session/CSRF-based and **not yet usable by
the mobile client**.

## Defined (implemented)
_None._

## Existing non-HTML endpoints (implemented, browser-oriented)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — triggers immediate poll of the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` of new-mail events (`ChatEventStream`/`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event JSON | 200 / 400 on bad signature |

Error handling: `GlobalExceptionHandler` renders HTML error pages; no JSON error
envelope exists. Unauthenticated requests get a 302 to `/login`.

## Planned (TBD — not implemented; shapes are proposals)
1. Token auth: `POST /api/v1/auth/login {email,password}` → `{accessToken, refreshToken, expiresIn}`; `POST /api/v1/auth/refresh`. Needs stateless filter chain for `/api/**` (CSRF off, bearer).
2. `GET /api/v1/chats?mailbox&q&filter&page` → page of conversation list items (mirror `ConversationListService`).
3. `GET /api/v1/chats/{key}` → bubble runs (mirror `ChatConversation`/`BubbleRun`).
4. `POST /api/v1/chats/{key}/send {body}` → via `OutboundMessageService`.
5. Unified error body `{ "error": code, "message": text }`.

Any change to this file must land with both repos' implementations in the same run.
