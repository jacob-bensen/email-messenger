# API contract

**Scope, and an important caveat.** `email-messenger` today is a
server-rendered Thymeleaf web app. Almost every HTTP endpoint returns
HTML (or an HTML fragment for morph-in-place updates), not JSON. There
is no REST API designed for third-party consumption yet — the sibling
mobile repo (`conexus-mail-mobile`) is empty and no mobile-facing API
contract has been agreed. This file inventories the endpoints that
exist and flags the only JSON/binary/streaming ones. When a mobile
API is defined, its contract lives in this file as its own section.

All authenticated endpoints require a session cookie from form login or
Google OIDC; there is no bearer-token auth today.

## Legend

- **HTML** — returns a full Thymeleaf-rendered page.
- **FRAG** — returns an HTML fragment (used by the in-place morph on
  `/chats`).
- **REDIRECT** — 3xx to another route.
- **JSON** — JSON body.
- **SSE** — `text/event-stream`.
- **BINARY** — image / manifest / webmanifest / other non-HTML asset.
- **AUTH** — session required. **PUBLIC** — no session.

## Public / landing

| Method | Path                    | Kind     | Notes                       |
| ------ | ----------------------- | -------- | --------------------------- |
| GET    | `/`                     | HTML     | landing / pricing page      |
| GET    | `/pricing`              | HTML     |                             |
| GET    | `/privacy`              | HTML     |                             |
| GET    | `/terms`                | HTML     |                             |
| GET    | `/refund`               | HTML     |                             |
| GET    | `/demo`                 | HTML     | interactive demo of chat UI |
| GET    | `/robots.txt`           | BINARY   | text/plain                  |
| GET    | `/sitemap.xml`          | BINARY   | application/xml             |
| GET    | `/images/og-card.png`   | BINARY   | image/png                   |

## Auth (PUBLIC unless noted)

| Method | Path                     | Kind     |
| ------ | ------------------------ | -------- |
| GET    | `/login`                 | HTML     |
| GET    | `/register`              | HTML     |
| POST   | `/register`              | REDIRECT |
| GET    | `/auth/google/start`     | REDIRECT |
| GET    | `/verify-email`          | HTML     |
| POST   | `/verify-email/resend`   | REDIRECT |
| GET    | `/password/forgot`       | HTML     |
| POST   | `/password/forgot`       | REDIRECT |
| GET    | `/password/reset`        | HTML     |
| POST   | `/password/reset`        | REDIRECT |

## Account (AUTH)

| Method | Path                        | Kind     |
| ------ | --------------------------- | -------- |
| GET    | `/account`                  | HTML     |
| GET    | `/account/notifications`    | HTML     |
| GET    | `/account/appearance`       | HTML     |
| POST   | `/account/appearance`       | REDIRECT |
| GET    | `/account/privacy`          | HTML     |
| POST   | `/account/password`         | REDIRECT |
| POST   | `/account/email`            | REDIRECT |

## Mailboxes (AUTH)

| Method | Path                                  | Kind     | Notes                                     |
| ------ | ------------------------------------- | -------- | ----------------------------------------- |
| GET    | `/mailboxes`                          | HTML     | list                                      |
| GET    | `/mailboxes/new`                      | HTML     | IMAP connect form                         |
| POST   | `/mailboxes`                          | REDIRECT | create IMAP mailbox                       |
| POST   | `/mailboxes/{id}/sync`                | REDIRECT | force poll                                |
| POST   | `/mailboxes/preferences`              | REDIRECT | e.g. digest opt-in                        |
| POST   | `/mailboxes/{id}/signature`           | REDIRECT | update outbound signature                 |
| POST   | `/mailboxes/{id}/delete`              | REDIRECT |                                           |
| GET    | `/mailboxes/gmail/connect`            | REDIRECT | Gmail OAuth start                         |
| GET    | `/mailboxes/gmail/reconnect/{id}`     | REDIRECT | refresh-token reconnect                   |
| GET    | `/mailboxes/gmail/callback`           | REDIRECT | Gmail OAuth callback                      |

## Chats / conversations (AUTH)

| Method | Path                                | Kind | Notes                                                                             |
| ------ | ----------------------------------- | ---- | --------------------------------------------------------------------------------- |
| GET    | `/chats`                            | HTML | conversation workspace (rail + open pane)                                         |
| GET    | `/chats/new`                        | HTML | compose form                                                                      |
| POST   | `/chats/new`                        | REDIRECT | send new outbound message                                                     |
| POST   | `/chats/refresh`                    | REDIRECT | force resync                                                                  |
| GET    | `/chats/{key}`                      | HTML | full page for one conversationKey                                                 |
| GET    | `/chats/{key}/pane`                 | FRAG | HTML fragment for in-place pane morph                                             |
| POST   | `/chats/{key}/send`                 | REDIRECT | send reply into conversation                                                  |
| GET    | `/chats/events`                     | SSE  | per-user event stream, currently emits `NewMailEvent` from polling / IMAP IDLE    |

### `GET /chats/events` (SSE)

The one live-data endpoint. Emits one event per new mail landing in the
signed-in user's mailboxes.

- `event: new-mail`
- `data`: a compact JSON payload describing what changed (currently a
  hint used by the browser to trigger a fragment refresh of the rail
  and pane; the pane fragment endpoint above is the actual data path).

If a mobile client is added, this event shape becomes the mobile
push-notification trigger and MUST be pinned in this doc.

Legacy redirects: `/threads`, `/threads/**`, `/senders` → `/chats`
equivalents.

## Saved searches (AUTH)

| Method | Path                             | Kind     |
| ------ | -------------------------------- | -------- |
| POST   | `/saved-searches`                | REDIRECT |
| POST   | `/saved-searches/{id}/delete`    | REDIRECT |

## Team (AUTH)

| Method | Path                        | Kind     |
| ------ | --------------------------- | -------- |
| GET    | `/team/invite`              | HTML     |
| POST   | `/team/invite`              | REDIRECT |
| GET    | `/team/invite/accept`       | HTML     |
| POST   | `/team/invite/accept`       | REDIRECT |

## Billing (AUTH unless noted)

| Method | Path                              | Kind     | Notes                                            |
| ------ | --------------------------------- | -------- | ------------------------------------------------ |
| POST   | `/billing/checkout`               | REDIRECT | to Stripe Checkout                               |
| POST   | `/billing/portal`                 | REDIRECT | to Stripe billing portal                         |
| GET    | `/billing/success`                | HTML     |                                                  |
| GET    | `/billing/cancel`                 | HTML     |                                                  |
| GET    | `/billing/cancel-subscription`    | HTML     |                                                  |
| POST   | `/billing/cancel-subscription`    | REDIRECT |                                                  |
| POST   | `/billing/webhook`                | JSON in  | **PUBLIC** — Stripe → server, `Stripe-Signature` |

Stripe webhook is the only endpoint that consumes JSON. It honors
exactly the four events named in the README.

## Digest

| Method | Path                | Kind | Notes                                    |
| ------ | ------------------- | ---- | ---------------------------------------- |
| GET    | `/digest/opt-out`   | HTML | PUBLIC, one-time signed link from digest |

## PWA / assets (PUBLIC)

| Method | Path                            | Kind    | Content-type                    |
| ------ | ------------------------------- | ------- | ------------------------------- |
| GET    | `/manifest.webmanifest`         | BINARY  | application/manifest+json       |
| GET    | `/icons/icon-192.png`           | BINARY  | image/png                       |
| GET    | `/icons/icon-512.png`           | BINARY  | image/png                       |
| GET    | `/icons/icon-512-maskable.png`  | BINARY  | image/png                       |
| GET    | `/apple-touch-icon.png`         | BINARY  | image/png                       |
| GET    | `/sw.js`                        | BINARY  | application/javascript          |
| GET    | `/offline`                      | HTML    | offline page for the SW         |

## Admin (AUTH + allowlist)

Access is gated by `admin.emails` in `application.yml`. Non-admins get
a 404.

| Method | Path                                              | Kind     |
| ------ | ------------------------------------------------- | -------- |
| GET    | `/admin/revenue`                                  | HTML     |
| POST   | `/admin/revenue/reconcile-billing-period`         | REDIRECT |
| POST   | `/admin/retention/win-back`                       | REDIRECT |

## When we design a mobile API

The following contract items will be needed and are not yet designed:

- Authentication that isn't a browser cookie (bearer / OIDC).
- JSON endpoints for: chats list, single conversation with bubble runs,
  send reply, mailbox connect / status, attachment download.
- Push token registration + delivery contract (server → device).
- A JSON version of the `NewMailEvent` payload that today only ships as
  an SSE hint.

Do not implement any of this without an explicit product decision — see
`INTERNAL_TODO.md`.
