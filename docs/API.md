# API (canonical contract)

Shared source of truth for endpoints exposed by the backend and consumed by
any client — today only the server-rendered website itself, tomorrow
potentially a mobile client (see the sibling repo `conexus-mail-mobile`).

## Current state (2026-08-29)

**No general-purpose REST/JSON API exists.** The backend is server-rendered
Thymeleaf; endpoints return HTML (or HTML fragments for htmx-style
partials). Two exceptions:

- `POST /billing/webhook` — Stripe webhook receiver (JSON in, `200 OK` out).
  Not for client use.
- `GET /chats/events` — Server-Sent Events stream (`text/event-stream`)
  publishing live mail events for the currently open chat view (see
  `ChatEventsController` and `ChatEventStream`). Consumed by
  `chat.html` in-browser.

Anything else a mobile client would need — a JSON list of chats, message
send, mailbox connect, auth — does not have a JSON surface yet. The
Thymeleaf routes below are HTML endpoints listed for orientation only;
they are **not** a client contract.

## HTML-only routes (reference, not a contract)

Grouped by controller. All return HTML unless noted.

### Public / marketing
- `GET /` (`SeoController`) — landing page
- `GET /pricing`
- `GET /demo`
- `GET /privacy`, `GET /terms`, `GET /refund`
- `GET /robots.txt`, `GET /sitemap.xml`, `GET /manifest.webmanifest`,
  `GET /sw.js`, `GET /offline`
- `GET /apple-touch-icon.png`, `GET /icons/icon-{192,512,512-maskable}.png`,
  `GET /images/og-card.png`

### Auth
- `GET /login`, `GET /register`, `POST /register`
- `GET /verify-email`, `POST /verify-email/resend`
- `GET /password/forgot`, `POST /password/forgot`
- `GET /password/reset`, `POST /password/reset`
- `GET /auth/google/start`
- `GET /account`, `GET /account/appearance`,
  `GET /account/notifications`, `GET /account/privacy`
- `POST /account/appearance`, `POST /account/email`, `POST /account/password`

### Mailboxes (`/mailboxes/**`)
- `GET /mailboxes/connect`, `GET /mailboxes/new`
- Gmail OAuth: `GET /mailboxes/gmail/callback`,
  `GET /mailboxes/gmail/reconnect/{id}`
- `POST /mailboxes/{id}/sync`, `POST /mailboxes/{id}/signature`,
  `POST /mailboxes/{id}/delete`

### Chats (the IM workspace)
- `GET /chats` — chat list rail
- `GET /chats/new`, `POST /chats/new`
- `GET /chats/{key}` — conversation view (participant-set key)
- `GET /chats/{key}/pane` — pane fragment for morphing updates
- `POST /chats/{key}/send`
- `POST /chats/refresh`
- `GET /chats/events` — **SSE stream** (`text/event-stream`)

### Legacy threads / senders
- `GET /threads`, `GET /threads/**`, `GET /senders`
  (routes into `ChatRedirectController`)

### Saved searches (`/searches/**`)
- Managed via `SavedSearchController`

### Teams
- `GET /team/invite`, `POST /team/invite`
- `GET /team/invite/accept`, `POST /team/invite/accept`

### Billing
- `POST /billing/checkout`, `POST /billing/portal`
- `GET /billing/success`, `GET /billing/cancel`
- `POST /billing/cancel-subscription`,
  `GET /billing/cancel-subscription`
- `POST /billing/webhook` — **JSON Stripe webhook**

### Digest
- `GET /digest/opt-out`

### Admin (operator-only, gated by `ADMIN_EMAILS`)
- `GET /admin/revenue`
- `POST /admin/retention/win-back`
- `POST /admin/revenue/reconcile-billing-period`

## Mobile-client contract (TBD)

Not defined. When a mobile client is scaffolded, add a new section per
resource here (auth, mailboxes, chats, messages, participants) with:

- Method + path + auth requirement
- Request JSON schema
- Response JSON schema
- Error codes and shapes

Until then, contract-first work for the mobile client blocks on defining
this section.
