# WEB

Inventory of the Thymeleaf website served by the Spring Boot backend.
Every page listed here is a `@Controller` returning a template name (or
a small `@ResponseBody` fragment for morph-in updates) — none of them
returns JSON.

Web and API paths **share the same service layer** — no forked business
logic. When a mobile endpoint lands in `docs/API.md`, both this page
and the mobile client must hit the same service (`ChatService`,
`ConversationListService`, `OutboundMessageService`, etc.).

## Layout

- Templates: `src/main/resources/templates/`
- Shared fragments: `src/main/resources/templates/fragments/`
  (nav, chat pane, install banner, live-refresh SSE handler,
  service-worker registration, SEO meta, cookie banner)
- Static assets: `src/main/resources/static/`
- Legal boilerplate source: `src/main/resources/legal/`
- Common model attributes injected by `SiteModelAdvice` and
  `NavModelAdvice`.

## Page inventory

### Marketing / public

| Route                       | Controller                                  | Template                     |
|-----------------------------|---------------------------------------------|------------------------------|
| `GET /`                     | `PricingController#landing`                 | `landing.html`               |
| `GET /pricing`              | `PricingController#pricing`                 | `pricing.html`               |
| `GET /demo`                 | `DemoController#demo`                       | `demo` (see controller)      |
| `GET /privacy`              | `LegalController#privacy`                   | `legal.html`                 |
| `GET /terms`                | `LegalController#terms`                     | `legal.html`                 |
| `GET /refund`               | `LegalController#refund`                    | `legal.html`                 |

### Auth

| Route                                | Controller                            | Notes                                        |
|--------------------------------------|---------------------------------------|----------------------------------------------|
| `GET/POST /login`                    | `AuthController` + Spring Security    | Form login; `login.html`                     |
| `GET/POST /register`                 | `AuthController#register`             | Sign-up; `register.html`                     |
| `GET /verify-email`                  | `EmailVerificationController#verify`  | Consumes a verification token                |
| `POST /verify-email/resend`          | `EmailVerificationController#resend`  | Rate-limited resend                          |
| `GET/POST /password/forgot`          | `PasswordResetController`             | Request a reset                              |
| `GET/POST /password/reset`           | `PasswordResetController`             | Consume a reset token                        |
| `GET /auth/google/start`             | `OAuthStartController`                | Trampoline into Spring Security OAuth flow   |
| `GET /account`                       | `AccountController#account`           | Account overview                             |
| `GET /account/notifications`         | `AccountController#notifications`     | Digest / notification prefs                  |
| `GET/POST /account/appearance`       | `AccountController#appearance`        | Theme / density prefs                        |
| `GET /account/privacy`               | `AccountController#privacy`           | Data export / delete controls                |
| `POST /account/password`             | `AccountController#changePassword`    | Password change                              |
| `POST /account/email`                | `AccountController#changeEmail`       | Email change + reverification                |

### Chat workspace

All under an authenticated session. `/threads` and `/senders` legacy
paths redirect to `/chats` via `ChatRedirectController`.

| Route                              | Controller                     | Purpose                                             |
|------------------------------------|--------------------------------|-----------------------------------------------------|
| `GET /chats`                       | `ConversationController#list`  | Conversation list; texting-style rail               |
| `GET /chats/new`                   | `ConversationController#newForm`| New-chat compose form                              |
| `POST /chats/new`                  | `ConversationController#create`| Start a new conversation                            |
| `POST /chats/refresh`              | `ConversationController#refresh`| Manual poll trigger                                |
| `GET /chats/{key}`                 | `ConversationController#open`  | Open one conversation                               |
| `GET /chats/{key}/pane`            | `ConversationController#pane`  | HTML fragment; used by SSE morph-in updates         |
| `POST /chats/{key}/send`           | `ConversationController#send`  | Send a reply (subject, body, attachments)           |
| `GET /chats/events`                | `ChatEventsController`         | SSE stream: `text/event-stream` of `NewMailEvent`s  |

### Mailboxes

| Route                                       | Controller                             | Purpose                              |
|---------------------------------------------|----------------------------------------|--------------------------------------|
| `GET /mailboxes`                            | `MailboxController#list`               | Connected-mailbox list               |
| `GET /mailboxes/new`                        | `MailboxController#newForm`            | Add IMAP mailbox form                |
| `POST /mailboxes`                           | `MailboxController#create`             | Connect via IMAP + password          |
| `POST /mailboxes/{id}/sync`                 | `MailboxController#sync`               | Force a poll                         |
| `POST /mailboxes/preferences`               | `MailboxController#preferences`        | Global mail prefs                    |
| `POST /mailboxes/{id}/signature`            | `MailboxController#signature`          | Update per-mailbox signature         |
| `POST /mailboxes/{id}/delete`               | `MailboxController#delete`             | Disconnect                           |
| `GET /mailboxes/gmail/connect`              | `GmailConnectController#connect`       | Start Gmail OAuth                    |
| `GET /mailboxes/gmail/reconnect/{id}`       | `GmailConnectController#reconnect`     | Refresh-token rotation flow          |
| `GET /mailboxes/gmail/callback`             | `GmailConnectController#callback`      | OAuth code exchange                  |

### Saved searches

| Route                             | Controller                          | Purpose                       |
|-----------------------------------|-------------------------------------|-------------------------------|
| `POST /searches`                  | `SavedSearchController#create`      | Save a search                 |
| `POST /searches/{id}/delete`      | `SavedSearchController#delete`      | Remove a saved search         |

### Teams

| Route                                 | Controller                             | Purpose                              |
|---------------------------------------|----------------------------------------|--------------------------------------|
| `GET /team/invite`                    | `TeamInviteController#invite`          | Invite form                          |
| `POST /team/invite`                   | `TeamInviteController#send`            | Send an invite                       |
| `GET /team/invite/accept`             | `TeamInviteController#acceptForm`      | Invite acceptance landing            |
| `POST /team/invite/accept`            | `TeamInviteController#accept`          | Join the team                        |

### Billing

| Route                                        | Controller                        | Purpose                              |
|----------------------------------------------|-----------------------------------|--------------------------------------|
| `POST /billing/checkout`                     | `BillingController#checkout`      | Redirect to Stripe Checkout          |
| `POST /billing/portal`                       | `BillingController#portal`        | Redirect to Stripe Billing Portal    |
| `GET /billing/success`                       | `BillingController#success`       | Post-checkout landing                |
| `GET /billing/cancel`                        | `BillingController#cancel`        | Checkout abandonment landing         |
| `GET/POST /billing/cancel-subscription`      | `BillingController`               | In-app cancel flow                   |
| `POST /billing/webhook`                      | `BillingWebhookController`        | Stripe webhook (JSON in, JSON out)   |

### Admin

Gated by `admin.emails`; non-admins get 404s.

| Route                                          | Controller                              | Purpose                          |
|------------------------------------------------|-----------------------------------------|----------------------------------|
| `GET /admin/revenue`                           | `AdminRevenueController#revenue`        | Metrics + at-risk queue          |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController#reconcile`      | Refresh billing-period cursor    |
| `POST /admin/retention/win-back`               | `AdminRevenueController#winBack`        | Fire the per-row win-back email  |

### Digest opt-out

| Route                       | Controller                     | Purpose                              |
|-----------------------------|--------------------------------|--------------------------------------|
| `GET /digest/opt-out`       | `DigestController#optOut`      | Unsubscribe from the digest email    |

### PWA & SEO

| Route                              | Controller                    | Notes                              |
|------------------------------------|-------------------------------|------------------------------------|
| `GET /manifest.webmanifest`        | `PwaController`               | PWA manifest                       |
| `GET /icons/icon-192.png`          | `PwaController`               | PWA icon                           |
| `GET /icons/icon-512.png`          | `PwaController`               | PWA icon                           |
| `GET /icons/icon-512-maskable.png` | `PwaController`               | PWA maskable icon                  |
| `GET /apple-touch-icon.png`        | `PwaController`               | iOS home-screen icon               |
| `GET /sw.js`                       | `PwaController`               | Service worker (offline shell)     |
| `GET /offline`                     | `PwaController`               | Offline fallback page              |
| `GET /robots.txt`                  | `SeoController`               | Robots directives                  |
| `GET /sitemap.xml`                 | `SeoController`               | Sitemap                            |
| `GET /images/og-card.png`          | `SeoController`               | Open-graph share card              |
