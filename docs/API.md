# API.md — canonical API contract

Status (2026-10-09): **no mobile/REST API exists yet.** The server is a session-cookie,
CSRF-protected, server-rendered app. The mobile repo (`conexus-mail-mobile`) is an empty
scaffold. This file documents the only machine-readable endpoints that exist today and
lists what is TBD. Do not implement client calls against anything not marked **Implemented**.

## Implemented JSON/stream endpoints (session-cookie auth; CSRF token required on POST except webhook)

### `POST /chats/refresh`
Auth: session + CSRF. Triggers a synchronous mailbox poll for the caller.
200 → `{"imported": <int>}`. Unauthenticated → 302 to `/login`.

### `GET /chats/events`
Auth: session. `text/event-stream`. Event `new-mail`, data `{"imported": <int>}`.
Comment `ping` every 25s (`mailbox.sse.keepalive-ms`). Emitter timeout 30 min; client reconnects.

### `POST /billing/webhook`
Auth: none; header `Stripe-Signature` required. 200 `ok`; 400 missing/invalid signature;
503 not configured; 500 processing failure (Stripe retries).

## TBD — `/api/v1` for mobile (not implemented)
Needed before any client work: token auth (login/refresh/logout), conversation list,
conversation detail (bubble runs), send/reply, mailbox list/sync, plan/limits, push-token
registration. Each must be added here first, then built on the existing services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`).
Error envelope: TBD (propose `{"error":{"code","message"}}`).
