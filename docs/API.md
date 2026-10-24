# API contract

**Status (2026-10-24): no dedicated mobile API exists.** This file is the
canonical contract. §1 documents what the server really exposes today
(verified against controllers); §2 is the planned `/api/v1` surface and is
**not implemented** — neither repo may rely on it until it moves to §1.

Conventions for everything here: all authenticated routes use the session
cookie today; mutating routes require a CSRF token (`_csrf` form field or
`X-CSRF-TOKEN`) unless noted.

## 1. Implemented endpoints that are not HTML pages

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session | none | `200 application/json` `{"imported": <int>}` — synchronous poll of the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`. Event `new-mail`, data `{"imported": <int>}`; `: ping` comments as keepalive |
| POST | `/billing/webhook` | none (Stripe signature) | raw Stripe JSON + `Stripe-Signature` header | `200 "ok"`, `400` missing/invalid signature, `500` apply failure (Stripe retries), `503` not configured. CSRF-exempt |
| GET | `/actuator/health` | none | | Spring Boot health |

Form-driven routes returning HTML/redirects (not an API; see `docs/WEB.md`):
`POST /chats/new`, `POST /chats/{key}/send` (multipart: `body` ≤100000 chars,
optional `subject`, `asReply`, `attachments[]`, `mailbox`), mailbox CRUD under
`/mailboxes`, etc.

Error shape: unspecified/TBD — errors currently render the `error.html`
template (`GlobalExceptionHandler`); oversized uploads → HTTP 413.

## 2. Planned `/api/v1` (NOT IMPLEMENTED)
Proposed first slice (to be built together with the client, one feature per run):
1. Token auth: `POST /api/v1/auth/login` → bearer token; CSRF-exempt `/api/**`.
2. `GET /api/v1/chats?filter=all|unread|awaiting|attachments&q=&mailbox=&page=`
   backed by `ConversationListService.list`.
3. `GET /api/v1/chats/{key}` and `POST /api/v1/chats/{key}/send` backed by
   `ChatService` / `OutboundMessageService`.
4. Realtime: reuse `/chats/events` with bearer auth.
Schemas will be fixed here before implementation.
