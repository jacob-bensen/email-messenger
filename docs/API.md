# API contract

**Status (2026-10-17): there is no REST API for the mobile client.** The backend is a
server-rendered Thymeleaf app with session-cookie auth. This file is the shared contract; the
mobile client must not call anything not marked *Implemented* here.

## 1. Implemented machine-readable endpoints

All require an authenticated session cookie unless stated. State-changing requests need the
CSRF token (`_csrf` param or `X-CSRF-TOKEN` header) except the Stripe webhook.

### `POST /chats/refresh`
Synchronously polls the user's due mailboxes. Auth: session. Response `200 application/json`:
```json
{ "imported": 3 }
```
`imported` = number of newly imported messages. Unauthenticated → `302 /login`.

### `GET /chats/events`
Server-Sent Events stream (`text/event-stream`) of new-mail notifications
(`NewMailEvent` via `ChatEventStream`). Event payload shape: TBD (read `ChatEventStream`).

### `POST /billing/webhook`
Stripe → server, signature-verified (`Stripe-Signature`), no CSRF, public. Not for clients.

## 2. Auth today

`POST /login` form params `email`, `password`, optional `remember-me` → `302` with
`JSESSIONID`. Google OIDC at `/auth/google/start`. No bearer tokens. **TBD** for mobile.

## 3. Proposed mobile API v1 — NOT IMPLEMENTED (design sketch, do not code against)

Should be built on the existing services (`ConversationListService`, `ChatService`,
`MailAccountService`) and view records, under `/api/v1`, JSON, bearer-token auth:

| Method | Path | Maps to existing | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/token` | `UserService`/login | needs token model (TBD) |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `ConversationListService.list` | items = `ConversationListItem`; `filter` ∈ ALL/UNREAD/AWAITING/ATTACHMENTS |
| GET | `/api/v1/chats/{key}` | `ChatService` → `ChatConversation` | marks read |
| POST | `/api/v1/chats/{key}/messages` | send logic — **currently in `ConversationController`, extract first** | body, asReply, subject, attachments |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` | |

Prerequisites are tracked in `INTERNAL_TODO.md`.

## 4. Errors

Web layer: `GlobalExceptionHandler` renders `error.html`. No JSON error envelope exists yet (TBD).
