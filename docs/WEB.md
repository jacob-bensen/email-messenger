# Website (Thymeleaf)

The website and the SaaS app share this Spring Boot application. All
pages are server-rendered Thymeleaf views under
`src/main/resources/templates`. Business logic lives in service classes
and is invoked from the controllers below.

## Shared plumbing

- `web/NavModelAdvice`, `web/SiteModelAdvice`, `web/LandingProperties`,
  `web/SiteProperties` — model advice populating the top nav, site
  chrome, and landing/pricing copy across every page.
- `templates/fragments/` — layout, nav, footer, live-refresh, and
  chat-rail fragments consumed by the pages below.

## Page inventory

### Marketing / public

| Path        | Controller                          | Template        |
| ----------- | ----------------------------------- | --------------- |
| `/`         | `web/PricingController#landing`     | `landing.html`  |
| `/pricing`  | `web/PricingController#pricing`     | `pricing.html`  |
| `/demo`     | `web/DemoController`                | `chat.html` (demo mode) |
| `/privacy`  | `web/LegalController#privacy`       | `legal.html`    |
| `/terms`    | `web/LegalController#terms`         | `legal.html`    |
| `/refund`   | `web/LegalController#refund`        | `legal.html`    |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `web/SeoController` | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, PWA icons | `web/PwaController` | — |

### Auth

| Path                       | Controller                                       |
| -------------------------- | ------------------------------------------------ |
| `/login`, `/register`      | `auth/AuthController`                            |
| `/auth/google/start`       | `auth/OAuthStartController`                      |
| `/verify-email(/resend)`   | `auth/EmailVerificationController`               |
| `/password/forgot`, `/password/reset` | `auth/PasswordResetController`        |

### Account

`auth/AccountController` renders `/account`, `/account/notifications`,
`/account/appearance`, `/account/privacy`, and handles password/email
changes via POST.

### Mailboxes

`web/MailboxController` under `/mailboxes/**` for list/add/sync/signature
/preferences/delete. `web/GmailConnectController` handles OAuth
connect/reconnect/callback for Gmail accounts.

### Chats (the core product)

All under `web/ConversationController`:

- `GET /chats` — the workspace (rail + open pane)
- `GET /chats/new`, `POST /chats/new` — compose a new conversation
- `POST /chats/refresh` — manual refresh
- `GET /chats/{key}` — full conversation page
- `GET /chats/{key}/pane` — HTMX-style pane fragment
- `POST /chats/{key}/send` — send a reply
- `GET /chats/events` — SSE stream (`web/ChatEventsController`,
  `web/ChatEventStream`)

Legacy `/threads`, `/threads/**`, `/senders` redirect to `/chats`
(`web/ChatRedirectController`).

### Saved searches

`web/SavedSearchController` under `/saved-searches/**` — create, list,
delete saved searches; counts computed by
`web/SavedSearchCountService`.

### Teams

`team/TeamInviteController` under `/team/invite(/accept)` for invite
flow (send, accept, view).

### Billing

`billing/BillingController` under `/billing/**` for checkout,
customer portal, success/cancel landings, and cancel-subscription flow.
`billing/BillingWebhookController` handles `POST /billing/webhook` for
Stripe events.

### Digest

`digest/DigestController` handles `GET /digest/opt-out` for unsubscribe
links in the weekly digest email.

### Admin

Gated by the `admin.emails` allowlist:

- `GET /admin/revenue` — revenue / churn / funnel dashboard
- `POST /admin/revenue/reconcile-billing-period` — backfill periods
- `POST /admin/retention/win-back` — trigger win-back outreach

## Rule

Web pages and any future REST API endpoints must share the SAME service
layer — never fork business logic between templates and controllers.
