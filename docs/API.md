# API — canonical contract

This is the shared source of truth between the backend (email-messenger) and the
mobile client (conexus-mail-mobile). If an endpoint isn't here, the client
must not depend on it; if the client needs something new, add it here first,
then implement both sides.

## Status

The backend today is almost entirely a **server-rendered Thymeleaf app**. There is
**no JSON REST API** for a mobile client. The mobile app cannot yet make useful
authenticated calls against this backend. Adding that surface is the top-priority
gap in `INTERNAL_TODO.md`.

The rest of this file has two sections:
1. **Existing web/HTML endpoints** — what the browser client calls today, for
   reference. Not consumable by the mobile client.
2. **Planned mobile REST API** — the contract we are moving toward.

---

## 1. Existing web/HTML endpoints (reference)

These render HTML (or perform a redirect after form submit). Auth is session-cookie
based, established by form login at `/login` or Google OIDC at `/auth/google/start`.

### Public / marketing / legal
- `GET /` — landing page (`PricingController#landing`)
- `GET /pricing` — pricing page
- `GET /demo` — public demo view
- `GET /privacy`, `GET /terms`, `GET /refund` — legal pages
- `GET /manifest.webmanifest`, `GET /sw.js`, `GET /icons/*` — PWA assets
- `GET /offline` — offline shell

### Auth
- `GET /login`, `GET /register` — forms
- `POST /register` — create account
- `GET /auth/google/start` — start Google OIDC (redirect)
- `GET /verify-email?token=…`, `POST /verify-email/resend`
- `GET /password/forgot`, `POST /password/forgot`
- `GET /password/reset?token=…`, `POST /password/reset`

### Account settings
- `GET /account`, `GET /account/notifications`, `GET /account/privacy`
- `GET /account/appearance`, `POST /account/appearance`
- `POST /account/password`, `POST /account/email`

### Chats (Thymeleaf, keyed by `conversationKey`)
- `GET /chats?page=…` — chat list
- `GET /chats/{key}` — full chat page
- `GET /chats/{key}/pane` — HTMX-friendly pane fragment
- `POST /chats/{key}/send` — send outbound reply
- `GET /chats/new`, `POST /chats/new` — compose
- `POST /chats/refresh` — **returns JSON** `{ "newMessages": <int> }`. The one
  ambient JSON endpoint in the app today.
- `GET /chats/events` — SSE stream (`text/event-stream`) that pushes `newMail`
  events for the signed-in user, consumed by the browser to morph the pane.
- `GET /threads`, `GET /senders`, `GET /threads/**` — legacy redirects → `/chats`

### Mailboxes
- `GET /mailboxes`, `GET /mailboxes/new`, `POST /mailboxes`
- `POST /mailboxes/{id}/sync`, `POST /mailboxes/{id}/delete`
- `POST /mailboxes/{id}/signature`, `POST /mailboxes/preferences`
- `GET /mailboxes/gmail/connect`, `GET /mailboxes/gmail/reconnect/{id}`,
  `GET /mailboxes/gmail/callback`

### Saved searches
- `POST /searches`, `POST /searches/{id}/delete`

### Team
- `GET /team/invite`, `POST /team/invite`
- `GET /team/invite/accept?token=…`, `POST /team/invite/accept`

### Billing
- `POST /billing/checkout`, `POST /billing/portal`
- `GET /billing/success`, `GET /billing/cancel`
- `GET /billing/cancel-subscription`, `POST /billing/cancel-subscription`
- `POST /billing/webhook` — Stripe webhook (JSON in, JSON out)

### Digest
- `GET /digest/opt-out?token=…`

### Admin
- `GET /admin/revenue`
- `POST /admin/revenue/reconcile-billing-period`
- `POST /admin/retention/win-back`

---

## 2. Planned mobile REST API (TBD)

Everything below is a design placeholder. Implementations will be added in future
runs; when they land, this section becomes real contract with request/response JSON
schemas and error codes, and the client MUST match it exactly.

### Design principles

- All mutations are server-authoritative and transactional (read → validate →
  mutate → publish `NewMailEvent` where applicable).
- The client's `lib/domain/` mirrors server rules but never trusts computed values
  from itself — on conflict, server wins.
- Content-Type is `application/json; charset=utf-8`.
- Error responses follow `{ "error": { "code": "<slug>", "message": "<human>" } }`.
- Auth: TBD — most likely **bearer token** (opaque, DB-backed, issued via
  `POST /api/v1/auth/login`) with refresh; do NOT reuse the browser session cookie
  for native clients. See `INTERNAL_TODO.md`.

### Base path

All mobile endpoints are versioned under `/api/v1/`.

### Planned resources (schemas TBD)

- `POST /api/v1/auth/login` — email+password → `{ token, refreshToken, user }`
- `POST /api/v1/auth/google` — exchange a Google ID token → same shape
- `POST /api/v1/auth/refresh` — refresh token → new access token
- `POST /api/v1/auth/logout` — revoke current token
- `GET  /api/v1/me` — current user + plan
- `GET  /api/v1/mailboxes` — list connected mailboxes
- `POST /api/v1/mailboxes/{id}/sync` — trigger a sync
- `GET  /api/v1/chats?cursor=…` — paginated chat list (rail)
- `GET  /api/v1/chats/{key}` — one conversation (bubble runs)
- `POST /api/v1/chats/{key}/send` — outbound reply
- `GET  /api/v1/events` — server-sent-events stream (parity with `/chats/events`)

Any endpoint added to the mobile surface MUST land here with its full schema before
the mobile client is allowed to depend on it.
