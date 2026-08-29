# WEB — Thymeleaf pages served by email-messenger

The backend renders the entire user-facing website as server-side
Thymeleaf. All business logic lives in the service layer; controllers
just adapt HTTP ↔ services ↔ views. The mobile client (when built) MUST
call the same services via the REST API — never fork logic into
controllers or templates.

## Route inventory

Grouped by controller. Method column is GET unless marked.

### Marketing / SEO / static

| Route                              | Controller                    | Template / response       |
| ---------------------------------- | ----------------------------- | ------------------------- |
| `/`                                | `PricingController`           | `landing.html`            |
| `/pricing`                         | `PricingController`           | `pricing.html`            |
| `/demo`                            | `DemoController`              | `demo` view / redirect    |
| `/privacy`                         | `LegalController`             | `legal.html`              |
| `/terms`                           | `LegalController`             | `legal.html`              |
| `/refund`                          | `LegalController`             | `legal.html`              |
| `/robots.txt`                      | `SeoController`               | plain text                |
| `/sitemap.xml`                     | `SeoController`               | XML                       |
| `/images/og-card.png`              | `SeoController`               | PNG                       |

### Auth

| Route                                        | Method | Controller                       |
| -------------------------------------------- | ------ | -------------------------------- |
| `/login`                                     | GET    | `AuthController`                 |
| `/register`                                  | GET, POST | `AuthController`               |
| `/verify-email`                              | GET    | `EmailVerificationController`    |
| `/verify-email/resend`                       | POST   | `EmailVerificationController`    |
| `/password/forgot`                           | GET, POST | `PasswordResetController`      |
| `/password/reset`                            | GET, POST | `PasswordResetController`      |
| `/auth/google/start`                         | GET    | `OAuthStartController`           |

### Account

| Route                                        | Method | Controller                       |
| -------------------------------------------- | ------ | -------------------------------- |
| `/account`                                   | GET    | `AccountController`              |
| `/account/notifications`                     | GET    | `AccountController`              |
| `/account/appearance`                        | GET, POST | `AccountController`            |
| `/account/privacy`                           | GET    | `AccountController`              |
| `/account/password`                          | POST   | `AccountController`              |
| `/account/email`                             | POST   | `AccountController`              |

### Mailboxes

| Route                                        | Method | Controller                       |
| -------------------------------------------- | ------ | -------------------------------- |
| `/mailboxes/new`                             | GET    | `MailboxController`              |
| `/mailboxes/{id}/sync`                       | POST   | `MailboxController`              |
| `/mailboxes/preferences`                     | POST   | `MailboxController`              |
| `/mailboxes/{id}/signature`                  | POST   | `MailboxController`              |
| `/mailboxes/{id}/delete`                     | POST   | `MailboxController`              |
| `/mailboxes/gmail/connect`                   | GET    | `GmailConnectController`         |
| `/mailboxes/gmail/reconnect/{id}`            | GET    | `GmailConnectController`         |
| `/mailboxes/gmail/callback`                  | GET    | `GmailConnectController`         |

### Chats (the IM workspace)

| Route                              | Method | Response                             |
| ---------------------------------- | ------ | ------------------------------------ |
| `/chats`                           | GET    | `chat.html` (full workspace)         |
| `/chats/new`                       | GET, POST | `compose.html`                    |
| `/chats/refresh`                   | POST   | JSON `{ "imported": int }`            |
| `/chats/{key}`                     | GET    | `chat.html`                          |
| `/chats/{key}/pane`                | GET    | `fragments/chat-pane` (partial)      |
| `/chats/{key}/send`                | POST   | redirect back to `/chats/{key}`      |
| `/chats/events`                    | GET    | SSE (`text/event-stream`)            |
| `/threads`, `/threads/**`, `/senders` | GET | 301/302 → `/chats` (`ChatRedirectController`) |

### Searches, teams

| Route                              | Method | Controller                        |
| ---------------------------------- | ------ | --------------------------------- |
| `/searches/{id}/delete`            | POST   | `SavedSearchController`           |
| `/team/invite`                     | GET, POST | `TeamInviteController`         |
| `/team/invite/accept`              | GET, POST | `TeamInviteController`         |

### Billing

| Route                                      | Method | Controller                    |
| ------------------------------------------ | ------ | ----------------------------- |
| `/billing/checkout`                        | POST   | `BillingController`           |
| `/billing/portal`                          | POST   | `BillingController`           |
| `/billing/success`                         | GET    | `BillingController`           |
| `/billing/cancel`                          | GET    | `BillingController`           |
| `/billing/cancel-subscription`             | GET, POST | `BillingController`         |
| `/billing/webhook`                         | POST   | `BillingWebhookController` (Stripe) |

### Digest / opt-out

| Route                              | Method | Controller                       |
| ---------------------------------- | ------ | -------------------------------- |
| `/digest/opt-out`                  | GET    | `DigestController`               |

### Admin (allowlisted)

| Route                                          | Method | Controller                       |
| ---------------------------------------------- | ------ | -------------------------------- |
| `/admin/revenue`                               | GET    | `AdminRevenueController`         |
| `/admin/revenue/reconcile-billing-period`      | POST   | `AdminRevenueController`         |
| `/admin/retention/win-back`                    | POST   | `AdminRevenueController`         |

### PWA

| Route                              | Method | Controller / response            |
| ---------------------------------- | ------ | -------------------------------- |
| `/manifest.webmanifest`            | GET    | `PwaController` (manifest+json)  |
| `/icons/icon-192.png`, `-512.png`, `-512-maskable.png` | GET | `PwaController` PNG |
| `/apple-touch-icon.png`            | GET    | `PwaController` PNG              |
| `/sw.js`                           | GET    | `PwaController` service worker   |
| `/offline`                         | GET    | `PwaController` (fallback page)  |

## Templates and fragments

`src/main/resources/templates/`:

- Top-level pages: `landing.html`, `pricing.html`, `login.html`,
  `register.html`, `verify-email.html`, `chat.html`,
  `conversation.html`, `compose.html`, `account.html`,
  `account-appearance.html`, `account-notifications.html`,
  `account-privacy.html`, `error.html`, `legal.html`.
- Subdirectories: `admin/`, `billing/`, `digest/`, `fragments/`,
  `mailboxes/`, `password/`, `team/`.
- `fragments/` holds reusable pieces (nav, chat-pane, live-refresh, …).
  Every controller that returns a partial (e.g. `/chats/{key}/pane`)
  references a fragment here.

## Sharing logic with the (future) mobile API

Every controller in `web/` delegates to a service in `service/`. When
we start implementing `/api/v1/*`, those REST controllers must call the
**same** services (e.g. `ChatService`, `ConversationListService`,
`ReplyService`, `MailAccountService`) — no duplication of validation,
threading, plan-limit checks, or persistence in either the web or the
API layer.
