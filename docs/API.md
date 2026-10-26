# API contract (canonical, shared with conexus-mail-mobile)

Status 2026-10-26: **no mobile REST API exists.** This file documents what is actually exposed today and lists the
planned v1 surface as TBD. Do not implement client code against the TBD section until it is specified here and built.

## Conventions today
- Auth: session cookie after `POST /login` (form fields `email`, `password`, optional `remember-me`; CSRF token required).
- CSRF: all non-GET requests need the CSRF token except `/billing/webhook`.
- Unauthenticated requests get a 302 to `/login` (not 401).

## Implemented JSON / stream endpoints
### `POST /chats/refresh` — auth required
Syncs the user's due mailboxes synchronously. Response `200 application/json`: `{"imported": <int>}`.
Errors: 302 → /login when unauthenticated; 403 on missing CSRF token.

### `GET /chats/events` — auth required, `text/event-stream`
SSE stream of new-mail events for the user (`ChatEventStream`, event published by `MailboxPollingService`).
Event payload schema: TBD (see `NewMailEvent`, `ChatEventStream`).

### `POST /billing/webhook` — public, Stripe signature header
Server-to-server only; not for the client.

## Planned v1 (TBD — not implemented)
Token auth (`POST /api/v1/auth/login`, refresh), conversations list/detail (`conversationKey`), send, mailbox list,
live events. Shapes to be defined contract-first in a later run; reuse `ConversationListService`, `ChatService`,
and a new send service extracted from `ConversationController`.
