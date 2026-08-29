# API — email-messenger

The canonical REST-API contract between the backend and the mobile
client (`conexus-mail-mobile`). Both repos MUST match this file
exactly. Update this file **before** implementing either side.

## Current state (2026-08-29)

**There is essentially no REST API today.** The backend is a
Thymeleaf-rendered web application that serves HTML for all
user-facing workflows. The only non-HTML endpoints that exist:

| Method | Path                | Response                 | Auth               |
| ------ | ------------------- | ------------------------ | ------------------ |
| POST   | `/chats/refresh`    | `{ "imported": <int> }`  | session (Principal) |
| GET    | `/chats/events`     | `text/event-stream` (SSE, `NewMailEvent`) | session |
| POST   | `/billing/webhook`  | Stripe webhook (server-to-server) | Stripe signature |
| GET    | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/…`, `/apple-touch-icon.png` | Static PWA assets | none |
| GET    | `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SEO/static | none |

Session auth is cookie-based (Spring Security form login or Google
OIDC). There is **no** bearer-token or OAuth-client-credentials flow
today. Every other route in the app returns HTML.

## Building the mobile-client contract

The mobile client cannot exist as a real product until the backend
exposes a JSON API and a client-friendly auth token. Rather than
guess DTO shapes, this section lists the endpoints the client will
need and the current status of each. Each row becomes a discrete
sync-routine item once we start implementing it.

### Auth

| Endpoint (planned)             | Purpose                                     | Status |
| ------------------------------ | ------------------------------------------- | ------ |
| POST `/api/v1/auth/login`      | Email + password → access + refresh token   | TBD    |
| POST `/api/v1/auth/refresh`    | Refresh token → new access token            | TBD    |
| POST `/api/v1/auth/google`     | Google ID token → access + refresh token    | TBD    |
| POST `/api/v1/auth/logout`     | Revoke the refresh token                    | TBD    |
| GET  `/api/v1/me`              | Current user profile + plan                 | TBD    |

Open decisions (record in `INTERNAL_TODO.md` when picked):
- Token format: opaque server-side (Spring Security OAuth2 resource
  server + a token store) vs. JWT signed by the backend.
- Refresh-token rotation policy.
- Do mobile sessions share the browser session (SSO cookie) or stay
  independent?

### Mailboxes

| Endpoint (planned)                     | Purpose                                         | Status |
| -------------------------------------- | ----------------------------------------------- | ------ |
| GET  `/api/v1/mailboxes`               | List the user's connected mail accounts         | TBD    |
| POST `/api/v1/mailboxes/imap`          | Add an IMAP-password account                    | TBD    |
| POST `/api/v1/mailboxes/gmail/start`   | Begin Gmail OAuth flow, return authorization URL | TBD   |
| POST `/api/v1/mailboxes/{id}/sync`     | Trigger a synchronous re-poll                   | TBD    |
| POST `/api/v1/mailboxes/{id}/signature`| Update the outbound signature                   | TBD    |
| DELETE `/api/v1/mailboxes/{id}`        | Disconnect a mailbox                            | TBD    |

### Chats

| Endpoint (planned)                              | Purpose                                       | Status |
| ----------------------------------------------- | --------------------------------------------- | ------ |
| GET  `/api/v1/chats?mailbox=&filter=&cursor=`   | Paged conversation list                       | TBD    |
| GET  `/api/v1/chats/{conversationKey}`          | Conversation timeline (bubbles, signatures)   | TBD    |
| POST `/api/v1/chats/{conversationKey}/read`     | Mark conversation read                        | TBD    |
| POST `/api/v1/chats/{conversationKey}/send`     | Send a reply                                  | TBD    |
| POST `/api/v1/chats/refresh`                    | Force-poll active mailboxes; returns `{imported}` | TBD |
| GET  `/api/v1/chats/events`                     | SSE stream of `NewMailEvent`                  | TBD    |

### Account / billing

| Endpoint (planned)                          | Purpose                                    | Status |
| ------------------------------------------- | ------------------------------------------ | ------ |
| GET  `/api/v1/account`                      | Profile, notification prefs, appearance    | TBD    |
| POST `/api/v1/account/notifications`        | Update notification prefs                  | TBD    |
| POST `/api/v1/billing/checkout`             | Create Stripe checkout session (mobile-safe redirect) | TBD |
| POST `/api/v1/billing/portal`               | Create Stripe portal session               | TBD    |

### Web Push (mobile push notifications)

| Endpoint (planned)                              | Purpose                                       | Status |
| ----------------------------------------------- | --------------------------------------------- | ------ |
| POST `/api/v1/push/subscriptions`               | Register a push subscription (VAPID or FCM)   | TBD    |
| DELETE `/api/v1/push/subscriptions/{id}`        | Unsubscribe                                   | TBD    |

## Conventions (proposed)

Once we start implementing:

- Base path: `/api/v1`. Bump the major version on breaking changes;
  no shims while pre-1.0.
- Requests/responses: `application/json; charset=utf-8`.
- Errors: RFC 7807 `application/problem+json` with `type`, `title`,
  `status`, `detail`, and an app-specific `code` string.
- Timestamps: RFC 3339 UTC (`2026-08-29T14:23:00Z`).
- IDs: opaque strings (numeric ID stringified for now to leave room).
- Auth: `Authorization: Bearer <accessToken>`, refreshed via
  `/api/v1/auth/refresh`.
- Pagination: `cursor` query param → `{ items, nextCursor }`.

Everything above is subject to change until the first endpoint ships.
Record any decision here before implementing it in either repo.
