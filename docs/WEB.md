# email-messenger — Thymeleaf website

Server-rendered pages under `src/main/resources/templates/`. Controllers
live in the packages listed alongside each route below. Business logic
runs in the service layer (`docs/BACKEND.md`); templates only format
values.

## Route inventory

### Public (no auth)

| Route                          | Template                       | Controller                                             |
|--------------------------------|--------------------------------|--------------------------------------------------------|
| `GET  /`                       | `landing.html`                 | `web.PricingController`                                |
| `GET  /pricing`                | `pricing.html`                 | `web.PricingController`                                |
| `GET  /demo`                   | `demo.html`                    | `web.DemoController`                                   |
| `GET  /login`                  | `login.html`                   | `auth.AuthController`                                  |
| `POST /login`                  | (Spring Security)              | `auth.SecurityConfig` form-login filter                |
| `GET  /register`               | `register.html`                | `auth.AuthController`                                  |
| `POST /register`               | redirect                       | `auth.AuthController`                                  |
| `GET  /auth/google/start`      | redirect                       | `auth.OAuthStartController`                            |
| `GET  /password/forgot`, `POST`| `password/…`                   | `auth.PasswordResetController`                         |
| `GET  /password/reset`,  `POST`| `password/…`                   | `auth.PasswordResetController`                         |
| `GET  /verify-email`           | `verify-email.html`            | `auth.EmailVerificationController`                     |
| `POST /verify-email/resend`    | redirect                       | `auth.EmailVerificationController`                     |
| `GET  /privacy`, `/terms`, `/refund` | `legal.html` variants    | `web.LegalController`                                  |
| `GET  /billing/cancel`         | `billing/…`                    | `billing.BillingController`                            |
| `POST /billing/webhook`        | JSON ack                       | `billing.BillingWebhookController`                     |
| `GET  /digest/opt-out`         | `digest/…`                     | `digest.DigestController`                              |
| `GET  /manifest.webmanifest`, PWA icons, `/sw.js`, `/offline` | (bytes) | `web.PwaController` |
| `GET  /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | (bytes) | `web.SeoController` |

### Authenticated (session cookie required)

| Route                                | Template / verb                     | Controller                     |
|--------------------------------------|-------------------------------------|--------------------------------|
| `GET  /chats`                        | `chat.html` (rail + pane)           | `web.ConversationController`   |
| `GET  /chats/new`, `POST`            | `compose.html`                      | `web.ConversationController`   |
| `POST /chats/refresh`                | redirect                            | `web.ConversationController`   |
| `GET  /chats/{key}`                  | `chat.html`                         | `web.ConversationController`   |
| `GET  /chats/{key}/pane`             | fragment                            | `web.ConversationController`   |
| `POST /chats/{key}/send`             | redirect                            | `web.ConversationController`   |
| `GET  /chats/events`                 | text/event-stream                   | `web.ChatEventsController`     |
| `GET  /threads`, `/senders`          | redirect → `/chats`                 | `web.ChatRedirectController`   |
| `GET/POST /mailboxes`, `/mailboxes/{id}/sync|delete|signature`, `/preferences`, `/new` | `mailboxes/*.html` | `web.MailboxController` |
| `GET  /mailboxes/gmail/connect|reconnect/{id}|callback` | redirects | `web.GmailConnectController` |
| `POST /searches`, `POST /searches/{id}/delete` | redirect              | `web.SavedSearchController`    |
| `GET  /account`, `/account/notifications|appearance|privacy` | `account*.html` | `auth.AccountController` |
| `POST /account/appearance|password|email` | redirect                       | `auth.AccountController`       |
| `GET  /team/invite`, `POST`; `/team/invite/accept`, `POST` | `team/*.html`  | `team.TeamInviteController`    |
| `POST /billing/checkout`, `POST /billing/portal`, `GET /billing/success`, `GET/POST /billing/cancel-subscription` | | `billing.BillingController` |

### Operator-only (email address in `ADMIN_EMAILS`)

| Route                                        | Controller                                 |
|----------------------------------------------|--------------------------------------------|
| `GET  /admin/revenue`                        | `admin.AdminRevenueController`             |
| `POST /admin/revenue/reconcile-billing-period` | `admin.AdminRevenueController`           |
| `POST /admin/retention/win-back`             | `admin.AdminRevenueController`             |

## Shared fragments / layout

- `templates/fragments/live-refresh.html` — SSE wiring for chat updates.
- `templates/fragments/` also carries reusable snippets used across
  chat, account, and admin pages.

## Sharing with the REST API

Where the website and the mobile REST API expose the same information
(e.g. plan catalog on `/pricing` and on `GET /api/v1/plans`), both
controllers MUST read from the same billing helper
(`PlanPricing`, `PlanLimits`) — no duplicated numeric literals in either
templates or DTO records.
