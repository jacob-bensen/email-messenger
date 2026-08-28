# WEB — Thymeleaf pages

Inventory of the browser-facing pages served by this backend. The mobile
client does NOT hit these; it will hit the JSON API described in
[API.md](API.md). Every page listed here must go through the same service
layer the API uses, so logic never forks between the two surfaces.

Templates live under `src/main/resources/templates/`. Shared fragments in
`templates/fragments/` (layout, nav, banners, live-refresh SSE wiring).

## Public marketing site

| Path        | Controller                  | Template             |
|-------------|-----------------------------|----------------------|
| `/`         | `PricingController`         | `landing.html`       |
| `/pricing`  | `PricingController`         | `pricing.html`       |
| `/demo`     | `DemoController`            | `demo` fragment      |
| `/privacy`  | `LegalController`           | `legal.html`         |
| `/terms`    | `LegalController`           | `legal.html`         |
| `/refund`   | `LegalController`           | `legal.html`         |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/…`, `/apple-touch-icon.png` | `PwaController` | — |

## Auth

| Path                        | Method | Controller                       |
|-----------------------------|--------|----------------------------------|
| `/login`                    | GET    | `AuthController`                 |
| `/login`                    | POST   | Spring Security default          |
| `/register`                 | GET    | `AuthController`                 |
| `/register`                 | POST   | `AuthController`                 |
| `/auth/google/start`        | GET    | `OAuthStartController`           |
| `/password/forgot`          | GET/POST | `PasswordResetController`     |
| `/password/reset`           | GET/POST | `PasswordResetController`     |
| `/verify-email`             | GET    | `EmailVerificationController`    |
| `/verify-email/resend`      | POST   | `EmailVerificationController`    |

## Signed-in app — chats surface

| Path                            | Method | Controller                    |
|---------------------------------|--------|-------------------------------|
| `/chats`                        | GET    | `ConversationController`      |
| `/chats/new`                    | GET/POST | `ConversationController`    |
| `/chats/refresh`                | POST   | `ConversationController`      |
| `/chats/{key}`                  | GET    | `ConversationController`      |
| `/chats/{key}/pane`             | GET    | `ConversationController` (fragment) |
| `/chats/{key}/send`             | POST   | `ConversationController`      |
| `/chats/events` (SSE)           | GET    | `ChatEventsController`        |
| `/threads`, `/threads/**`, `/senders` | GET | `ChatRedirectController` (→ `/chats`) |
| `/searches/{id}/delete`         | POST   | `SavedSearchController`       |

## Mailbox management

| Path                                    | Method | Controller                |
|-----------------------------------------|--------|---------------------------|
| `/mailboxes/new`                        | GET    | `MailboxController`       |
| `/mailboxes/{id}/sync`                  | POST   | `MailboxController`       |
| `/mailboxes/preferences`                | POST   | `MailboxController`       |
| `/mailboxes/{id}/signature`             | POST   | `MailboxController`       |
| `/mailboxes/{id}/delete`                | POST   | `MailboxController`       |
| `/mailboxes/gmail/connect`              | GET    | `GmailConnectController`  |
| `/mailboxes/gmail/reconnect/{id}`       | GET    | `GmailConnectController`  |
| `/mailboxes/gmail/callback`             | GET    | `GmailConnectController`  |

## Account settings

| Path                       | Method | Controller           |
|----------------------------|--------|----------------------|
| `/account`                 | GET    | `AccountController`  |
| `/account/notifications`   | GET    | `AccountController`  |
| `/account/appearance`      | GET/POST | `AccountController` |
| `/account/privacy`         | GET    | `AccountController`  |
| `/account/password`        | POST   | `AccountController`  |
| `/account/email`           | POST   | `AccountController`  |

## Billing

| Path                                  | Method | Controller                     |
|---------------------------------------|--------|--------------------------------|
| `/billing/checkout`                   | POST   | `BillingController`            |
| `/billing/portal`                     | POST   | `BillingController`            |
| `/billing/success`                    | GET    | `BillingController`            |
| `/billing/cancel`                     | GET    | `BillingController`            |
| `/billing/cancel-subscription`        | GET/POST | `BillingController`          |
| `/billing/webhook`                    | POST   | `BillingWebhookController` (Stripe-signed, `@RestController`) |

## Teams

| Path                       | Method | Controller               |
|----------------------------|--------|--------------------------|
| `/team/invite`             | GET/POST | `TeamInviteController` |
| `/team/invite/accept`      | GET/POST | `TeamInviteController` |

## Digest opt-out

| Path                | Method | Controller           |
|---------------------|--------|----------------------|
| `/digest/opt-out`   | GET    | `DigestController`   |

## Admin

| Path                                             | Method | Controller                |
|--------------------------------------------------|--------|---------------------------|
| `/admin/revenue`                                 | GET    | `AdminRevenueController`  |
| `/admin/revenue/reconcile-billing-period`        | POST   | `AdminRevenueController`  |
| `/admin/retention/win-back`                      | POST   | `AdminRevenueController`  |

## Fragments and layout

- `templates/fragments/layout.html` — base HTML shell (head, nav, footer,
  cookie banner, dark-mode toggle).
- `templates/fragments/live-refresh.html` — SSE client + in-place morph
  used by `chat.html` to react to `/chats/events`.
- `templates/fragments/upgrade-modal.html` — plan-limit paywall used by
  paid features across the app.

## Model advice

- `NavModelAdvice` — active-tab hints for the nav.
- `SiteModelAdvice` — site properties (support email, brand name) exposed
  to every template.
- `LandingProperties` / `LandingVideo` — landing-page copy and hero clip.
