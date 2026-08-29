# API — email-messenger (ConexusMail)

## Status

**There is no JSON REST API today.** Every HTTP endpoint below returns
either HTML, an HTMX fragment, a redirect, an SSE stream, or a binary
asset — designed for the Thymeleaf website. Any mobile or third-party
client will need a JSON API surface built alongside a token-based auth
flow. This file is the canonical contract *once such an API exists*;
until then, the mobile client has nothing to call.

See `INTERNAL_TODO.md` for the prerequisites (JSON DTOs, auth token
issuance, error envelope, versioning).

## Surface inventory (server-rendered, for reference)

The current controllers are listed here so the future JSON API knows
which service methods it must expose in parity. Grouped by responsibility.

### Public / marketing (Thymeleaf HTML)

- `GET  /`                       — landing (`PricingController`)
- `GET  /pricing`                — pricing page
- `GET  /demo`                   — interactive demo
- `GET  /privacy`, `/terms`, `/refund` — legal pages
- `GET  /robots.txt`, `/sitemap.xml`, `/images/og-card.png` — SEO
- `GET  /manifest.webmanifest`, `/icons/*`, `/apple-touch-icon.png`,
        `/sw.js`, `/offline` — PWA shell

### Auth (form, session cookie)

- `GET  /login`
- `GET  /register`, `POST /register`
- `GET  /auth/google/start`      — Google OIDC handoff
- `GET  /password/forgot`, `POST /password/forgot`
- `GET  /password/reset`, `POST /password/reset`
- `GET  /verify-email`, `POST /verify-email/resend`

### Account settings (session cookie)

- `GET  /account`
- `GET  /account/notifications`
- `GET  /account/appearance`, `POST /account/appearance`
- `GET  /account/privacy`
- `POST /account/password`
- `POST /account/email`

### Mailboxes

- `GET  /mailboxes`, `GET /mailboxes/new`
- `POST /mailboxes/{id}/sync`
- `POST /mailboxes/preferences`
- `POST /mailboxes/{id}/signature`
- `POST /mailboxes/{id}/delete`
- `GET  /mailboxes/gmail/connect`
- `GET  /mailboxes/gmail/reconnect/{id}`
- `GET  /mailboxes/gmail/callback`

### Chats (the app)

- `GET  /chats`                        — rail + welcome pane
- `GET  /chats/{key}`                  — full page for one conversation
- `GET  /chats/{key}/pane`             — HTMX fragment: just the pane
- `POST /chats/{key}/send`             — send reply
- `GET  /chats/new`, `POST /chats/new` — compose new chat
- `POST /chats/refresh`                — manual refresh (rail)
- `GET  /chats/events` (SSE)           — `NewMailEvent` stream keyed by
  the current session's user; payload contains conversationKey so the
  client knows which pane/row to morph
- `GET  /threads`, `/threads/**`, `/senders` — redirect to `/chats` for
  legacy URLs

### Searches, teams, digest, billing, admin

- `GET|POST /searches`, `POST /searches/{id}/delete`
- `GET  /team/invite`, `POST /team/invite`,
  `GET /team/invite/accept`, `POST /team/invite/accept`
- `GET  /digest/opt-out`
- `POST /billing/checkout`, `POST /billing/portal`,
  `GET  /billing/success`, `GET /billing/cancel`,
  `GET  /billing/cancel-subscription`,
  `POST /billing/cancel-subscription`
- `POST /billing/webhook` — Stripe (raw JSON, HMAC-verified; not a
  general API)
- `GET  /admin/revenue`,
  `POST /admin/revenue/reconcile-billing-period`,
  `POST /admin/retention/win-back`

## Contract format (to be adopted when the JSON API lands)

Once endpoints are added, each entry in this file should be:

```
### <METHOD> /api/v1/<path>

Auth: <session | bearer | none>

Path params:
  - name (type): description

Query params:
  - name (type, required?): description

Request body:
  { "field": "type — description" }

Response 200:
  { "field": "type — description" }

Errors:
  - 400 <code>: <when>
  - 401 <code>: <when>
  - 403 <code>: <when>
  - 404 <code>: <when>
```

Conventions to lock in before writing the first endpoint:

- Path prefix `/api/v1/`.
- Auth: opaque bearer tokens issued via a new
  `POST /api/v1/auth/login` (returns `{token, expiresAt, user}`), plus
  a Google-OIDC exchange endpoint. Reuse the existing `UserService`.
- Timestamps: ISO-8601 UTC (`2026-08-29T14:03:00Z`).
- IDs: server-assigned bigints as strings.
- Errors: uniform `{code, message, details?}` envelope with a stable
  `code` enum (drives client i18n).
- Pagination: cursor-based (`?cursor=...&limit=...`).
- Every mutation runs in a transaction inside the existing service
  layer — no controller-only logic.
