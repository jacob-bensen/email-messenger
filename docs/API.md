# API contract

**Status: no mobile REST API is implemented.** Nothing under `/api/**` exists. The server's only HTTP surface is the Thymeleaf website (see `docs/WEB.md`) plus two machine-facing endpoints below. The Flutter client repo (`conexus-mail-mobile`) is an empty scaffold and calls nothing.

Rule for future work: this file is edited **before** code. Endpoints listed under "Planned" are TBD and not implemented.

## Implemented machine-facing endpoints

### `POST /billing/webhook`
Stripe webhook. Public, CSRF-exempt, authenticated by Stripe signature header (invalid signature → `InvalidStripeSignatureException`). Not for the mobile client.

### `GET /chats/events`
Server-Sent Events, `text/event-stream`. Requires a session-authenticated user (cookie). Emits a signal when new mail is imported for that user; the web UI refreshes the rail/pane in place. Event payload schema: TBD (see `ChatEventStream` / `NewMailEvent`).

## Planned (TBD, not implemented)
Minimum surface a mobile client needs, to be designed in a future run:
- Token auth (login/refresh/logout) — TBD, mechanism undecided (session cookie vs bearer).
- Conversation list, conversation detail (bubble runs), send/reply, mark read.
- Mailbox list/connect status; plan/limits summary.
- Error envelope: TBD. Existing web errors go through `GlobalExceptionHandler` (HTML).
