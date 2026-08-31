# Website — Thymeleaf pages

The Spring Boot app double-duties as the ConexusMail marketing site AND the
authenticated web app. Every page below is server-rendered from
`src/main/resources/templates/`. Business logic MUST come from the same
service beans the (future) REST API will use — no forked logic in
controllers or templates.

## Layouts and fragments

- `fragments/head.html`, `fragments/nav.html`, `fragments/footer.html` —
  shared chrome.
- `fragments/live-refresh.html` — SSE morphing helpers used by the chat
  pages to swap the rail and the open pane in place on `NewMailEvent`.
- `SiteModelAdvice`, `NavModelAdvice` — `@ControllerAdvice` beans that put
  site-wide model attributes (branding, nav state, current user) on every
  request.

## Public / marketing routes

| Route                        | Controller               | Template          |
| ---------------------------- | ------------------------ | ----------------- |
| `GET /`                      | `SeoController`          | `landing.html`    |
| `GET /pricing`               | `PricingController`      | `pricing.html`    |
| `GET /demo`                  | `DemoController`         | `chat.html` (canned data) |
| `GET /terms`, `/privacy`, `/refund` | `LegalController` | `legal.html`      |
| `GET /login`, `/register`    | `AuthController`         | `login.html`, `register.html` |
| `GET /verify-email`          | `EmailVerificationController` | `verify-email.html` |
| `GET /password/forgot`, `/password/reset` | `PasswordResetController` | `password/*.html` |
| `GET /robots.txt`, `/sitemap.xml` | `SeoController`     | text/xml          |

## PWA shell

Served by `PwaController`:

| Route                          | Purpose                          |
| ------------------------------ | -------------------------------- |
| `GET /manifest.webmanifest`    | PWA manifest                     |
| `GET /sw.js`                   | Service worker                   |
| `GET /offline`                 | Offline fallback                 |
| `GET /icons/*`, `/apple-touch-icon.png`, `/images/og-card.png` | Assets |

## Authenticated app

The core workspace is `/chats`.

| Route                            | Controller                | Notes |
| -------------------------------- | ------------------------- | ----- |
| `GET /chats`                     | `ConversationController`  | Rail + empty pane |
| `GET /chats/{key}`               | `ConversationController`  | Full page for one chat |
| `GET /chats/{key}/pane`          | `ConversationController`  | Fragment: pane only, for hx-morph swap |
| `POST /chats/{key}/send`         | `ConversationController`  | Reply via `ReplyService` |
| `GET /chats/refresh`             | `ConversationController`  | Fragment: rail only |
| `GET /chats/new`, `POST /chats/new` | `ConversationController` | Compose to new participants |
| `GET /chats/events`              | `ChatEventsController` + `ChatEventStream` | SSE stream |
| `GET /threads`, `/threads/**`    | `ChatRedirectController`  | Legacy route → new `/chats/{key}` |

## Mailboxes

Rooted at `/mailboxes`, handled by `MailboxController` and
`GmailConnectController`:

| Route                                  | Purpose                             |
| -------------------------------------- | ----------------------------------- |
| `GET /mailboxes`                       | List connected mailboxes            |
| `POST /mailboxes/connect`              | Add IMAP+SMTP mailbox               |
| `GET /mailboxes/gmail`                 | Start Gmail OAuth                   |
| `GET /mailboxes/gmail/callback`        | OAuth callback                      |
| `GET /mailboxes/gmail/reconnect/{id}`  | Weekly reconnect (see roadmap)      |
| `POST /mailboxes/{id}/sync`            | Kick a manual poll                  |
| `POST /mailboxes/{id}/signature`       | Save signature                      |
| `POST /mailboxes/{id}/delete`          | Disconnect                          |

## Search

| Route                       | Controller                | Notes |
| --------------------------- | ------------------------- | ----- |
| `GET /searches`             | `SavedSearchController`   | List + create |
| `POST /searches`, `DELETE /searches/{id}` | `SavedSearchController` | Mutate |
| `GET /senders`              | `SavedSearchController`   | People-view |

## Account & preferences

Handled by `AccountController`:

| Route                           | Purpose                          |
| ------------------------------- | -------------------------------- |
| `GET /account`                  | Overview                         |
| `GET /account/appearance`       | Theme, accent colors             |
| `GET /account/email`            | Change email                     |
| `GET /account/notifications`    | Web push opt-in (future)         |
| `GET /account/password`         | Change password                  |
| `GET /account/privacy`          | Data controls                    |
| `GET /preferences`              | Mail preferences (digests etc.)  |

## Billing

Handled by `BillingController` + `BillingWebhookController`:

| Route                              | Purpose                            |
| ---------------------------------- | ---------------------------------- |
| `POST /billing/checkout`           | Start Stripe Checkout              |
| `GET /billing/success`             | Post-checkout landing              |
| `GET /billing/cancel`              | Aborted checkout landing           |
| `POST /billing/portal`             | Redirect to Stripe portal          |
| `POST /billing/cancel-subscription`| One-click cancel with reason       |
| `POST /billing/webhook`            | Stripe webhook receiver            |

## Team

`TeamInviteController`:

| Route                        | Purpose            |
| ---------------------------- | ------------------ |
| `POST /team/invite`          | Invite a teammate  |
| `GET /team/invite/accept`    | Accept an invite   |

## Admin

`AdminRevenueController` — behind role check:

| Route                                              | Purpose              |
| -------------------------------------------------- | -------------------- |
| `GET /admin/revenue`                               | Revenue dashboard    |
| `POST /admin/revenue/reconcile-billing-period`     | Manual reconciliation|
| `GET /admin/retention/win-back`                    | Win-back campaign UI |

## Digest

`DigestController`:

| Route                    | Purpose                        |
| ------------------------ | ------------------------------ |
| `GET /digest/opt-out`    | Unsubscribe from digest email  |

## Error handling

`GlobalExceptionHandler` maps common exceptions to `error.html`. Domain-
specific errors (`BillingException`, `EmailImportException`, IMAP failures)
render inline messages on the relevant page.
