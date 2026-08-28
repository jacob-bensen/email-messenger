# WEB

The Thymeleaf website served by this backend. Same JVM, same service
layer, same database. Any REST API added for the mobile client MUST reuse
the same services these pages already call — never fork business logic
between web and API paths.

## Layout

- Templates: `src/main/resources/templates/`.
- Shared fragments: `templates/fragments/` (`app-nav`, `chat-pane`,
  `cookie-banner`, `install-banner`, `live-refresh`, `seo`,
  `settings-nav`, `sw-register`).
- Static assets: `src/main/resources/static/`.
- CSS lives alongside the templates and is loaded via `<link>` in
  `fragments/seo` or per-page.

## Global advice

- `NavModelAdvice` — populates nav model on every request.
- `SiteModelAdvice` — populates site model (branding, PWA install banner,
  cookie banner).
- `GlobalExceptionHandler` — renders `error.html`.

## Page inventory

Marketing and legal (public):

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /`                              | `LandingController` (via SeoController area) | Landing page (`landing.html`).  |
| `GET /pricing`                       | `PricingController`                 | Plans + CTA (`pricing.html`).      |
| `GET /demo`                          | `DemoController`                    | Static demo conversation.          |
| `GET /privacy`, `/terms`, `/refund`  | `LegalController`                   | Legal boilerplate.                 |
| `GET /robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, `/apple-touch-icon.png`, `/icons/*` | `SeoController` / `PwaController` | SEO + PWA shell.          |

Auth:

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /login`, `POST /login`          | Spring Security + `AuthController`  | Form login.                        |
| `GET /register`, `POST /register`    | `AuthController`                    | Form registration.                 |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` |                                    |
| `GET /password/forgot`, `POST /password/forgot`  | `PasswordResetController`     | Request reset link.                |
| `GET /password/reset`, `POST /password/reset`    | `PasswordResetController`     | Consume reset link.                |
| `GET /auth/google/start`             | `OAuthStartController`              | Kick off Google OIDC + intent.     |

Account:

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /account`                       | `AccountController`                 | Profile summary.                   |
| `GET /account/appearance`, `POST /account/appearance` | `AccountController`        | Theme + accents.                   |
| `GET /account/notifications`         | `AccountController`                 | Notification prefs.                |
| `GET /account/privacy`               | `AccountController`                 | Privacy prefs.                     |
| `POST /account/email`                | `AccountController`                 | Change email address.              |
| `POST /account/password`             | `AccountController`                 | Change password.                   |

Chats (the primary workspace):

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /chats`                         | `ConversationController`            | Rail + open pane.                  |
| `GET /chats/{key}`                   | `ConversationController`            | Focused conversation.              |
| `GET /chats/{key}/pane`              | `ConversationController`            | Partial pane fragment (HTMX-ish).  |
| `POST /chats/{key}/send`             | `ConversationController` → `ReplyService` | Send reply.                  |
| `GET /chats/new`, `POST /chats/new`  | `ConversationController`            | Start a new conversation.          |
| `POST /chats/refresh`                | `ConversationController`            | Manual poll.                       |
| `GET /chats/events` (SSE)            | `ChatEventsController` + `ChatEventStream` | Live updates.               |
| `GET /threads`, `/threads/**`, `/senders` | `ChatRedirectController`       | Legacy → `/chats` redirect.        |

Mailboxes:

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /mailboxes`                     | `MailboxController`                 | Mailbox list.                      |
| `GET /mailboxes/new`, `POST /mailboxes` | `MailboxController`              | Connect IMAP mailbox.              |
| `POST /mailboxes/{id}/delete`        | `MailboxController`                 |                                    |
| `POST /mailboxes/{id}/sync`          | `MailboxController`                 |                                    |
| `POST /mailboxes/{id}/signature`     | `MailboxController`                 | Update signature.                  |
| `GET /mailboxes/gmail/connect`, `/callback`, `/reconnect/{id}` | `GmailConnectController` | Gmail OAuth flow. |

Team + saved searches + digests:

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /team/invite`, `POST /team/invite` | Team controller                  | Invite by email.                   |
| `GET /team/invite/accept`, `POST /team/invite/accept` | Team controller     | Accept invite.                     |
| `GET /searches`, `POST /searches`, plus item routes | `SavedSearchController` | Saved-search CRUD.               |
| `GET /digest/opt-out`                | Digest controller                   | One-click opt out.                 |
| `POST /preferences`                  | Preferences controller              |                                    |

Billing:

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `POST /billing/checkout`             | `BillingController`                 | Stripe Checkout redirect.          |
| `POST /billing/portal`               | `BillingController`                 | Stripe Customer Portal.            |
| `GET /billing/success`, `/billing/cancel` | `BillingController`            | Post-checkout return pages.        |
| `GET /billing/cancel-subscription`, `POST /billing/cancel-subscription` | `BillingController` | Cancel flow.        |
| `POST /billing/webhook`              | `BillingWebhookController`          | Stripe → server (JSON, signed).    |

Admin (operator-only, gated by `ADMIN_EMAILS`):

| Route                                | Controller                          | Notes                              |
| ------------------------------------ | ----------------------------------- | ---------------------------------- |
| `GET /admin/revenue`                 | `AdminRevenueController`            | Metrics dashboard.                 |
| `POST /admin/retention/win-back`     | `AdminRevenueController`            | Per-row win-back CTA (EPIC-18 M1). |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController`  |                                    |

## Shared service contract

Templates call services in `com.emailmessenger.service` (chat) and
`com.emailmessenger.email` (mailbox). When a REST endpoint is added for
the mobile client, it MUST invoke the same service methods — no duplicated
transform, threading, or send logic.
