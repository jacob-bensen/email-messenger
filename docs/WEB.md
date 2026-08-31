# WEB.md — ConexusMail Thymeleaf website

Inventory of the server-rendered site. Every route below is served by a
Thymeleaf `@Controller` under `com.emailmessenger.web` (or a sibling
package such as `auth/`, `billing/`, `team/`, `admin/`, `digest/`). All
share the base layout in `templates/fragments/layout.html` and the CSS in
`static/css/main.css`.

## Global layout, model advice, PWA shell

- `NavModelAdvice` and `SiteModelAdvice` populate the shared model
  attributes (current user, plan hints, appearance, nav highlights) for
  every page.
- `templates/fragments/sw-register.html` is included on the layout to
  register the PWA service worker.
- `web/GlobalExceptionHandler` renders friendly error pages.
- `web/PwaController` serves `/manifest.webmanifest`, `/icons/*`,
  `/apple-touch-icon.png`, `/sw.js`, `/offline`.

## Page inventory

### Marketing / public

| Route              | Method | Controller           |
| ------------------ | ------ | -------------------- |
| `/`                | GET    | `web/PricingController` (landing) |
| `/pricing`         | GET    | `web/PricingController`           |
| `/demo`            | GET    | `web/DemoController`              |
| `/privacy`         | GET    | `web/LegalController`             |
| `/terms`           | GET    | `web/LegalController`             |
| `/refund`          | GET    | `web/LegalController`             |
| `/robots.txt`      | GET    | `web/SeoController`               |
| `/sitemap.xml`     | GET    | `web/SeoController`               |
| `/images/og-card.png` | GET | `web/SeoController` (generated OG image) |
| `/offline`         | GET    | `web/PwaController` (PWA fallback) |

### Auth

| Route                        | Method     | Controller                          |
| ---------------------------- | ---------- | ----------------------------------- |
| `/login`                     | GET/POST   | Spring Security form login          |
| `/register`                  | GET/POST   | `auth/AuthController`               |
| `/logout`                    | POST       | Spring Security                     |
| `/auth/google/start`         | GET        | `auth/OAuthStartController`         |
| `/oauth2/authorization/**`   | GET        | Spring Security OAuth2 client       |
| `/login/oauth2/code/**`      | GET        | Spring Security OAuth2 client       |
| `/password/forgot`           | GET/POST   | `auth/PasswordResetController`      |
| `/password/reset`            | GET/POST   | `auth/PasswordResetController`      |
| `/verify-email`              | GET        | `auth/EmailVerificationController`  |
| `/verify-email/resend`       | POST       | `auth/EmailVerificationController`  |

### Account / settings

| Route                    | Method   | Controller                |
| ------------------------ | -------- | ------------------------- |
| `/account`               | GET      | `auth/AccountController`  |
| `/account/notifications` | GET/POST | `auth/AccountController`  |
| `/account/appearance`    | GET/POST | `auth/AccountController`  |
| `/account/privacy`       | GET      | `auth/AccountController`  |
| `/account/password`      | POST     | `auth/AccountController`  |
| `/account/email`         | POST     | `auth/AccountController`  |

### Mailboxes

| Route                                  | Method   | Controller                    |
| -------------------------------------- | -------- | ----------------------------- |
| `/mailboxes`                           | GET      | `web/MailboxController`       |
| `/mailboxes/new`                       | GET/POST | `web/MailboxController`       |
| `/mailboxes/{id}/sync`                 | POST     | `web/MailboxController`       |
| `/mailboxes/preferences`               | POST     | `web/MailboxController`       |
| `/mailboxes/{id}/signature`            | POST     | `web/MailboxController`       |
| `/mailboxes/{id}/delete`               | POST     | `web/MailboxController`       |
| `/mailboxes/gmail/connect`             | GET      | `web/GmailConnectController`  |
| `/mailboxes/gmail/reconnect/{id}`      | GET      | `web/GmailConnectController`  |
| `/mailboxes/gmail/callback`            | GET      | `web/GmailConnectController`  |

### Chats workspace (the core product)

| Route                        | Method   | Controller                                  |
| ---------------------------- | -------- | ------------------------------------------- |
| `/chats`                     | GET      | `web/ConversationController` (list + rail)  |
| `/chats/new`                 | GET/POST | `web/ConversationController` (compose)      |
| `/chats/{conversationKey}`   | GET      | `web/ConversationController` (full page)    |
| `/chats/{conversationKey}/pane` | GET   | `web/ConversationController` (fragment)     |
| `/chats/{conversationKey}/send` | POST  | `web/ConversationController`                |
| `/chats/refresh`             | POST     | `web/ConversationController` (JSON heartbeat) |
| `/chats/events`              | GET      | `web/ChatEventsController` (SSE)            |
| `/threads`, `/threads/**`, `/senders` | GET | `web/ChatRedirectController` (legacy redirects) |

### Saved searches

| Route                     | Method | Controller                       |
| ------------------------- | ------ | -------------------------------- |
| `/searches`               | POST   | `web/SavedSearchController`      |
| `/searches/{id}/delete`   | POST   | `web/SavedSearchController`      |

### Team

| Route                    | Method   | Controller                    |
| ------------------------ | -------- | ----------------------------- |
| `/team/invite`           | GET/POST | `team/TeamInviteController`   |
| `/team/invite/accept`    | GET/POST | `team/TeamInviteController`   |

### Billing

| Route                              | Method   | Controller                    |
| ---------------------------------- | -------- | ----------------------------- |
| `/billing/checkout`                | POST     | `billing/BillingController`   |
| `/billing/portal`                  | POST     | `billing/BillingController`   |
| `/billing/success`                 | GET      | `billing/BillingController`   |
| `/billing/cancel`                  | GET      | `billing/BillingController`   |
| `/billing/cancel-subscription`     | GET/POST | `billing/BillingController`   |
| `/billing/webhook`                 | POST     | `billing/BillingWebhookController` (Stripe) |

### Digest

| Route              | Method | Controller             |
| ------------------ | ------ | ---------------------- |
| `/digest/opt-out`  | GET    | `digest/DigestController` |

### Admin (gated by `AdminAuthorizer`)

| Route                                              | Method | Controller                        |
| -------------------------------------------------- | ------ | --------------------------------- |
| `/admin/revenue`                                   | GET    | `admin/AdminRevenueController`    |
| `/admin/revenue/reconcile-billing-period`          | POST   | `admin/AdminRevenueController`    |
| `/admin/retention/win-back`                        | POST   | `admin/AdminRevenueController`    |

## Shared fragments

- `templates/fragments/layout.html` — page shell (head, nav, footer).
- `templates/fragments/nav.html` — top nav + active-tab logic.
- `templates/fragments/sw-register.html` — PWA service worker registration.
- `templates/fragments/live-refresh.html` — client-side SSE consumer for
  `/chats/events`; morphs the chats rail and open pane in place.
- `templates/fragments/onboarding.html` — checklist widget consumed by
  `OnboardingService` + `OnboardingChecklist`.

## Rule: shared service layer

Every controller — Thymeleaf or (future) REST — MUST route through the
services under `service/`, `email/`, `billing/`, `team/`, `auth/`, etc.
Business logic never lives in controllers or templates. The mobile
client's JSON API (see `docs/API.md`) will re-use these same services so
the two never drift.
