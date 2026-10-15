# API.md — canonical contract

**Status (2026-10-15): the server exposes NO versioned JSON REST API for the mobile client.** This file documents what exists and marks the rest TBD. Any mobile feature must first add its endpoints here, then implement both sides.

## Conventions today
- Auth: session cookie after `POST /login` (form-encoded `email`, `password`, `_csrf`). CSRF token required on all POSTs except `/billing/webhook`. Bearer tokens: **TBD / not implemented**.
- Errors: HTML `error.html` pages with status 404 (missing), 502 (mail/billing upstream failure), 400 (`IllegalArgumentException`). No JSON error envelope: **TBD**.

## Non-HTML endpoints that exist
### `POST /chats/refresh`
Auth: session + CSRF. Synchronously polls the user's mailboxes.
200: `{"imported": <int>}`

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE) of new-mail notifications per user (`ChatEventStream`, fed by `NewMailEvent`). Event names/payload shape: **TBD** (read `ChatEventStream` before relying on them).

### `POST /billing/webhook`
Auth: none; Stripe signature header `Stripe-Signature`. 200 `ok`; 400 missing/invalid signature; 503 not configured; 500 apply failure (Stripe retries). Server-to-server only.

## Form/HTML endpoints
All other routes return HTML or redirects; inventory in `WEB.md`.

## Planned mobile API (NOT implemented — TBD design)
Needed minimum: token auth, `GET /api/v1/conversations` (list + filters/search/paging), `GET /api/v1/conversations/{key}` (bubble runs), `POST /api/v1/conversations/{key}/messages`, mailbox list/connect, push/live events. Define schemas here before coding.
