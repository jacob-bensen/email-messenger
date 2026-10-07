# API.md — canonical API contract

**Status (2026-10-07): there is no mobile REST API.** The backend is a server-rendered Thymeleaf app
with session-cookie auth. This file documents the machine-facing endpoints that exist today and the
open design questions for a future `/api/v1`. The mobile repo (`conexus-mail-mobile`) is an empty
Flutter scaffold and consumes nothing yet. Any change here must land with both implementations.

## Existing machine-facing endpoints

Auth for all of these: session cookie from `POST /login` (form: `email`, `password`, `_csrf`).
CSRF token required on POSTs except `/billing/webhook`. Unauthenticated requests are redirected (302) to `/login`.

### `POST /chats/refresh`
Foreground heartbeat; synchronously polls the user's due mailboxes.
- Response `200 application/json`: `{ "imported": <int> }` (newly imported messages).
- Errors: 302 → `/login` if unauthenticated; 403 on missing CSRF.

### `GET /chats/events`
Server-Sent Events stream (`text/event-stream`) of live mail events for the signed-in user.
- Event `new-mail`, data `{"imported": <int>}`.
- Keepalive: SSE comment `ping` every 25 s (`mailbox.sse.keepalive-ms`).
- Events only fire for mailboxes with IMAP IDLE (paid plans) or on poll imports.

### `POST /billing/webhook`
Stripe → server. Public, CSRF-exempt, authenticated by the Stripe signature header.
Not for client use.

## Planned `/api/v1` — TBD (nothing below is implemented)

Open decisions, to be settled before any endpoint is added:
1. **Auth**: token scheme for mobile (e.g. short-lived access token + refresh token issued by
   `POST /api/v1/auth/login`; Google sign-in via ID-token exchange). Today only cookie sessions exist.
2. **Conversation read model**: DTOs mirroring `ChatConversation` / `BubbleRun` / `ConversationListItem`
   (`service` package), keyed by `conversationKey`.
3. **Send**: wrap `ReplyService` / the `/chats/{key}/send` logic (extract from `ConversationController` first).
4. **Plan limits**: expose `PlanLimits` (Free 3 mailboxes / 500 threads; Pro 5 / unlimited; Business unlimited)
   and map `PlanLimitExceededException` to a stable error code.
5. **Errors**: uniform `{ "error": "<code>", "message": "<text>" }`.
