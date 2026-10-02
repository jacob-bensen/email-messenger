# API contract

**Status: no mobile REST API exists yet.** This file is the canonical contract;
it will be filled in *before* any endpoint is implemented. Until then, only the
endpoints below are JSON/SSE (all others are server-rendered HTML, see `WEB.md`).

## Auth (applies to everything below)
Session cookie (`JSESSIONID` / remember-me) + CSRF token. Not usable from a native
client — a bearer-token scheme is TBD (INTERNAL_TODO #1).

## Existing non-HTML endpoints

### POST /chats/refresh
Auth: session + CSRF. Polls the user's active mailboxes now.
Response 200: `{"imported": <int>}`.

### GET /chats/events
Auth: session. `text/event-stream` (SSE). Emits new-mail notifications for the
user (`NewMailEvent`, published by `MailboxPollingService`). Event payload shape: TBD
(read `ChatEventStream` when documenting).

### POST /billing/webhook
Auth: Stripe signature (`Stripe-Signature`), CSRF-exempt. Server-to-server only.

## Planned v1 (not implemented — TBD, subject to review)
Auth token issue/refresh, conversation list, conversation messages, send, mark
read, mailbox list. Error envelope: TBD.
