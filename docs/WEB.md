# Website (Thymeleaf)

The backend serves the public website and the authenticated web app from
the same Spring Boot process. Views live under
`src/main/resources/templates/`, styles/JS under `src/main/resources/static/`.

Every controller listed here calls into the same service layer the REST
API in `docs/API.md` will call. **Do not fork business logic between web
and API paths.**

## Global fragments

- `fragments/app-nav.html` — top nav for authenticated pages.
- `fragments/settings-nav.html` — sub-nav under `/account/*`.
- `fragments/chat-pane.html` — chat message pane, embeddable.
- `fragments/live-refresh.html` — client-side JS that subscribes to
  `/chats/events` (SSE) and morphs the rail / open chat in place.
- `fragments/sw-register.html` — registers the PWA service worker.
- `fragments/install-banner.html`, `fragments/cookie-banner.html`,
  `fragments/seo.html`.

## Public pages

| Path                | Controller                  | Notes                                |
| ------------------- | --------------------------- | ------------------------------------ |
| `GET /`             | `PricingController.landing` | Landing page (`landing.html`).       |
| `GET /pricing`      | `PricingController.pricing` | Free / Pro / Business.               |
| `GET /demo`         | `DemoController`            | Static demo of the chat UI.          |
| `GET /privacy`      | `LegalController`           | From `resources/legal/`.             |
| `GET /terms`        | `LegalController`           |                                      |
| `GET /refund`       | `LegalController`           |                                      |
| `GET /robots.txt`   | `SeoController`             |                                      |
| `GET /sitemap.xml`  | `SeoController`             |                                      |
| `GET /images/og-card.png` | `SeoController`       | Programmatically rendered.           |
| `GET /manifest.webmanifest`, `/sw.js`, `/icons/**`, `/offline` | `PwaController` | PWA shell. |

## Auth pages

| Path                          | Controller                     | Notes |
| ----------------------------- | ------------------------------ | ----- |
| `GET  /login`                 | `AuthController`               | Form + Google button.        |
| `GET  /register`, `POST /register` | `AuthController`         | Creates a user, sends verify email. |
| `POST /login`                 | Spring Security                | Uses `email` + `password` params. |
| `GET  /auth/google/start`     | `OAuthStartController`         | Preserves `plan` intent through OAuth. |
| `GET  /verify-email`          | `EmailVerificationController`  | Verifies via `token` query param. |
| `POST /verify-email/resend`   | `EmailVerificationController`  |                                     |
| `GET  /password/forgot`, `POST /password/forgot` | `PasswordResetController` |                          |
| `GET  /password/reset`, `POST /password/reset`   | `PasswordResetController` |                          |
| `POST /logout`                | Spring Security                | Redirects to `/login?logout`. |

## Account pages

| Path                              | Controller                | Notes |
| --------------------------------- | ------------------------- | ----- |
| `GET  /account`                   | `AccountController`       | Overview + billing summary.  |
| `GET  /account/notifications`     | `AccountController`       | Notification prefs.           |
| `GET  /account/appearance`, `POST /account/appearance` | `AccountController` | Theme + accent. |
| `GET  /account/privacy`           | `AccountController`       |                                |
| `POST /account/password`          | `AccountController`       | Change password.               |
| `POST /account/email`             | `AccountController`       | Change email (verify flow).    |

## Chats (the authenticated workspace)

| Path                             | Controller                          | Notes |
| -------------------------------- | ----------------------------------- | ----- |
| `GET  /chats`                    | `ConversationController.list`       | Rail + selected pane.           |
| `GET  /chats/new`                | `ConversationController.newForm`    | Compose a new chat.             |
| `POST /chats/new`                | `ConversationController.create`     |                                  |
| `POST /chats/refresh` (JSON)     | `ConversationController.refresh`    | Manual pull; returns HTML fragment. |
| `GET  /chats/{key}`              | `ConversationController.open`       | Full page.                       |
| `GET  /chats/{key}/pane`         | `ConversationController.pane`       | Fragment for live morph.         |
| `POST /chats/{key}/send`         | `ConversationController.reply`      | SMTP send via `ReplyService`.    |
| `GET  /chats/events`             | `ChatEventsController`              | SSE, `text/event-stream`.        |
| `GET  /threads`, `/senders` (redirects) | `ChatRedirectController`     | Legacy → `/chats`.               |

## Mailboxes

| Path                                        | Controller                | Notes |
| ------------------------------------------- | ------------------------- | ----- |
| `GET  /mailboxes`                            | `MailboxController`       | List.                            |
| `GET  /mailboxes/new`, `POST /mailboxes`     | `MailboxController`       | IMAP-password connect.           |
| `POST /mailboxes/{id}/sync`                  | `MailboxController`       | Manual sync trigger.             |
| `POST /mailboxes/preferences`                | `MailboxController`       | Global mailbox preferences.      |
| `POST /mailboxes/{id}/signature`             | `MailboxController`       | Per-mailbox signature.           |
| `POST /mailboxes/{id}/delete`                | `MailboxController`       |                                  |
| `GET  /mailboxes/gmail/connect`              | `GmailConnectController`  | Start Gmail OAuth.               |
| `GET  /mailboxes/gmail/reconnect/{id}`       | `GmailConnectController`  | Weekly reconnect (see CLAUDE.md).|
| `GET  /mailboxes/gmail/callback`             | `GmailConnectController`  | OAuth code exchange.             |

## Saved searches

| Path                              | Controller                | Notes |
| --------------------------------- | ------------------------- | ----- |
| `POST /searches`                  | `SavedSearchController`   | Create.                          |
| `POST /searches/{id}/delete`      | `SavedSearchController`   |                                  |

## Billing

| Path                                   | Controller                     | Notes |
| -------------------------------------- | ------------------------------ | ----- |
| `POST /billing/checkout`               | `BillingController`            | Creates Stripe Checkout Session. |
| `POST /billing/portal`                 | `BillingController`            | Stripe Customer Portal.          |
| `GET  /billing/success`                | `BillingController`            |                                    |
| `GET  /billing/cancel`                 | `BillingController`            |                                    |
| `GET  /billing/cancel-subscription`, `POST /billing/cancel-subscription` | `BillingController` | Cancel with reason. |
| `POST /billing/webhook`                | `BillingWebhookController`     | Stripe → us. Raw JSON, no CSRF.  |

## Teams

| Path                                       | Controller             | Notes |
| ------------------------------------------ | ---------------------- | ----- |
| `GET  /team/invite`, `POST /team/invite`   | `TeamInviteController` | Owner sends an invite.           |
| `GET  /team/invite/accept`, `POST /team/invite/accept` | `TeamInviteController` | Invitee accepts. |

## Digest

| Path                       | Controller           | Notes |
| -------------------------- | -------------------- | ----- |
| `GET /digest/opt-out`      | `DigestController`   | Public, token-scoped.            |

## Admin (operator)

| Path                                          | Controller                       | Notes |
| --------------------------------------------- | -------------------------------- | ----- |
| `GET  /admin/revenue`                          | `AdminRevenueController`         | Revenue, churn, at-risk queue.   |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController`         | Backfill helper.                 |
| `POST /admin/retention/win-back`               | `AdminRevenueController`         | Send templated win-back email.   |

Access is gated by `AdminAuthorizer` (email allow-list).

## Layout & style

The chat workspace lives in `chat.html` (three-pane layout with rail,
pane, and signature rail). `landing.html` and `pricing.html` are the
marketing pages. CSS is under `static/css/` and JS under `static/js/`.
Dark mode and accent colors are user-selectable on
`/account/appearance` and stored on the `User` row.
