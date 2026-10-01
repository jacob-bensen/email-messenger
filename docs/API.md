# API contract

Canonical contract shared with `conexus-mail-mobile`. Last audited 2026-10-01.

**Status: no mobile REST API exists yet.** Only the endpoints below return non-HTML data.
Section "Planned" is a proposal (TBD, not implemented); do not code clients against it until
it moves to "Implemented".

## Conventions (current)
- Auth: session cookie; unauthenticated requests are redirected (302) to `/login`.
- All non-GET requests need a CSRF token (`_csrf` param or `X-CSRF-TOKEN` header) except the Stripe webhook.
- Errors: HTML error page via `GlobalExceptionHandler` (no JSON error envelope yet — TBD).

## Implemented
### POST `/chats/refresh`
Auth: session. Triggers a synchronous poll of the user's mailboxes.
Response `200 application/json`: `{ "imported": <int> }`

### GET `/chats/events`
Auth: session. `text/event-stream` of new-mail notifications (`NewMailEvent`, published by
`MailboxPollingService`). Payload schema: TBD (see `ChatEventStream`).

### POST `/billing/webhook`
Auth: none; Stripe signature header. Server-to-server only.

## Planned (TBD — proposal)
Under `/api/v1`, bearer-token auth, JSON errors `{ "error": code, "message": text }`:
- `POST /api/v1/auth/token`, `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`,
  `POST /api/v1/conversations/{key}/messages`, `GET /api/v1/mailboxes`.
Schemas to be defined when the first one is built (contract first).
