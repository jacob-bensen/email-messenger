# WEB.md — Thymeleaf website

The ConexusMail backend serves the full public website AND the authenticated
web app from the same Spring Boot process. Templates live under
`src/main/resources/templates/`, static assets under
`src/main/resources/static/`. All pages call the same service layer as the
REST API — controllers are thin.

## Page inventory

### Marketing / anonymous

| Route               | Template               | Controller                | Services used |
| ------------------- | ---------------------- | ------------------------- | ------------- |
| `/`                 | `landing.html`         | `web/DemoController`+     | `LandingProperties`, `LandingVideo` |
| `/pricing`          | `pricing.html`         | `web/PricingController`   | `billing/PlanCatalog` |
| `/demo`             | `landing.html` (demo)  | `web/DemoController`      | `service/DemoConversationService` |
| `/privacy`, `/terms`, `/refund` | `legal.html` | `web/LegalController` | `LegalProperties` |
| `/robots.txt`, `/sitemap.xml` | (raw) | `web/SeoController`      | — |

### Auth

| Route                 | Template            | Controller                          |
| --------------------- | ------------------- | ----------------------------------- |
| `/login`              | `login.html`        | `auth/AuthController`               |
| `/register`           | `register.html`     | `auth/AuthController`               |
| `/verify-email`       | `verify-email.html` | `auth/EmailVerificationController`  |
| `/password/forgot`    | `password/forgot.html` | `auth/PasswordResetController`   |
| `/password/reset`     | `password/reset.html`  | `auth/PasswordResetController`   |
| `/auth/google/start`  | — (redirect)        | `auth/OAuthStartController`         |

### Authenticated chats app

| Route                          | Template                | Controller                    |
| ------------------------------ | ----------------------- | ----------------------------- |
| `/chats`, `/chats/{key}`       | `chat.html`             | `web/ConversationController`  |
| `/chats/events` (SSE)          | — (SSE stream)          | `web/ChatEventsController`    |
| `/chats/{key}/reply`           | `chat.html` (fragment)  | `web/ConversationController`  |
| `/chats/{key}/compose`         | `compose.html`          | `web/ConversationController`  |
| `/conversations/{id}` (legacy) | `conversation.html`     | `web/ConversationController`  |

### Account & preferences

| Route                          | Template                        | Controller               |
| ------------------------------ | ------------------------------- | ------------------------ |
| `/account`                     | `account.html`                  | `auth/AccountController` |
| `/account/appearance`          | `account-appearance.html`       | `auth/AccountController` |
| `/account/notifications`       | `account-notifications.html`    | `auth/AccountController` |
| `/account/privacy`             | `account-privacy.html`          | `auth/AccountController` |

### Mailboxes

| Route                                        | Template                | Controller                 |
| -------------------------------------------- | ----------------------- | -------------------------- |
| `/mailboxes`, `/mailboxes/new`               | `mailboxes/*.html`      | `web/MailboxController`    |
| `/mailboxes/gmail/connect`, `/mailboxes/gmail/reconnect/{id}` | (redirect) | `web/GmailConnectController` |

### Billing

| Route                                  | Template                           | Controller                        |
| -------------------------------------- | ---------------------------------- | --------------------------------- |
| `/billing/checkout`, `/billing/portal` | (redirect to Stripe)               | `billing/BillingController`       |
| `/billing/success`                     | `billing/success.html`             | `billing/BillingController`       |
| `/billing/cancel`                      | `billing/cancel-subscription.html` | `billing/BillingController`       |
| `/billing/webhook` (POST, no CSRF)     | —                                  | `billing/StripeWebhookController` |

### Team

| Route                                  | Template                | Controller                        |
| -------------------------------------- | ----------------------- | --------------------------------- |
| `/team`, `/team/invite`                | `team/invite.html`      | `team/TeamController`             |
| `/team/accept`                         | `team/accept.html`      | `team/TeamController`             |

### Admin

| Route                                  | Template                | Controller                        |
| -------------------------------------- | ----------------------- | --------------------------------- |
| `/admin/revenue`                       | `admin/revenue.html`    | `admin/AdminController`           |

### PWA + digests

- `/manifest.webmanifest`, `/sw.js`, `/icons/**`, `/apple-touch-icon.png` are
  served as static assets and wired through `fragments/sw-register.html`.
- `/digest/opt-out` unsubscribes the user from digest emails
  (`digest/DigestController`).

## Shared fragments

Under `templates/fragments/`:

- `app-nav.html` — top nav for the authenticated app.
- `settings-nav.html` — side nav on `/account/*` pages.
- `chat-pane.html` — the message-thread pane, morphed in place on SSE events.
- `live-refresh.html` — SSE bootstrap for the chats view.
- `install-banner.html`, `cookie-banner.html`, `seo.html`,
  `sw-register.html` — cross-cutting UI.

## Rule

Web templates never talk to repositories directly. All data comes through the
same service classes the REST API uses (see `docs/BACKEND.md`), so the mobile
client and the website behave identically for anything a user would notice.
