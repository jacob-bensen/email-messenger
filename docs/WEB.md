# WEB.md — Thymeleaf website inventory

The `email-messenger` service is BOTH the API host for the mobile
client AND the public/logged-in website. This file catalogs the
website pages and the services they call — so a mobile-side change to
a shared service is not accidentally regressed here.

Rule: pages and (future) `/api/v1/**` endpoints MUST call the SAME
service methods. Never fork logic between a controller and a template.

## Page inventory (2026-08-30)

Marketing & legal (public):

| Path        | Template            | Controller           |
|-------------|---------------------|----------------------|
| `/`         | `landing.html`      | `ConversationController` (root redirect + landing) |
| `/pricing`  | `pricing.html`      | `PricingController`  |
| `/demo`     | `templates/…`       | `DemoController`     |
| `/privacy`, `/terms`, `/refund` | `legal.html` | `LegalController` (env-overridable via `MARKETING_LEGAL_*`) |

Auth flows:

| Path                        | Template              | Controller                    |
|-----------------------------|-----------------------|-------------------------------|
| `/login`                    | `login.html`          | `AuthController`              |
| `/register`                 | `register.html`       | `AuthController`              |
| `/verify-email`, `/verify-email/resend` | `verify-email.html` | `EmailVerificationController` |
| `/password/forgot`, `/password/reset` | `password/…` | `PasswordResetController`     |
| `/auth/google/start`        | (redirect)            | `OAuthStartController`        |

Logged-in app:

| Path                              | Template               | Controller                                                     |
|-----------------------------------|------------------------|----------------------------------------------------------------|
| `/chats`                          | `chat.html`            | `ConversationController`                                       |
| `/chats/new` (GET/POST)           | `compose.html`         | `ConversationController`                                       |
| `/chats/{key}`                    | `chat.html` (server-morphed) | `ConversationController`                               |
| `/chats/{key}/pane`               | fragment               | `ConversationController`                                       |
| `/chats/{key}/send` (POST)        | (redirect)             | `ConversationController`                                       |
| `/chats/refresh` (POST)           | (redirect)             | `ConversationController`                                       |
| `/chats/events` (SSE)             | (`text/event-stream`)  | `ChatEventsController`                                         |
| `/threads`, `/threads/**`, `/senders` | (redirect)         | `ChatRedirectController` — legacy paths → `/chats`             |
| `/mailboxes/**`                   | `mailboxes/…`          | `MailboxController`                                            |
| `/mailboxes/gmail/**` (connect + reconnect) | `mailboxes/…` | `GmailConnectController`                                       |
| `/searches/**`                    | (fragments)            | `SavedSearchController`                                        |
| `/account`, `/account/appearance`, `/account/notifications`, `/account/privacy`, `/account/email` (POST), `/account/password` (POST) | `account*.html` | `AccountController` |
| `/team/invite`, `/team/invite/accept` | `team/…`           | `TeamInviteController`                                         |
| `/digest/opt-out`                 | `digest/…`             | `DigestController`                                             |
| `/preferences` (POST)             | (redirect)             | user prefs — see `AccountController` / `UserMailPreferences`   |

Billing:

| Path                          | Controller                    |
|-------------------------------|-------------------------------|
| `/billing/checkout` (POST), `/billing/portal` (POST), `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription` (GET/POST) | `BillingController` |
| `/billing/webhook` (POST)     | `BillingWebhookController` (Stripe signature-verified) |

Admin:

| Path                                     | Controller                     |
|------------------------------------------|--------------------------------|
| `/admin/revenue`                         | `AdminRevenueController`       |
| `/admin/revenue/reconcile-billing-period` (POST) | `AdminRevenueController` |
| `/admin/retention/win-back` (POST)       | `AdminRevenueController`       |

PWA / SEO / static:

| Path                                              | Controller       |
|---------------------------------------------------|------------------|
| `/manifest.webmanifest`, `/sw.js`, `/offline`     | `PwaController`  |
| `/icons/icon-192.png`, `/icons/icon-512.png`, `/icons/icon-512-maskable.png`, `/apple-touch-icon.png`, `/images/og-card.png` | `PwaController` / `SeoController` |
| `/robots.txt`, `/sitemap.xml`                     | `SeoController`  |

## Shared fragments

- `templates/fragments/live-refresh.html` — SSE consumer used by
  `chat.html` for in-place morph of rail + open pane on new mail.
- `templates/fragments/…` — nav, layout, avatar strip, etc. (Full
  inventory not yet catalogued; add to this file as new fragments are
  extracted from pages.)

## Anti-drift checklist (for every new/changed page)

1. Are you calling a service method (never repository directly from a
   controller)? Same service the API will call?
2. Is business logic in the service, not in the template or the
   controller?
3. If this touches something the mobile app also uses, is the
   change reflected in `docs/API.md` in the same run?
4. If the change touches CSS or JS, does the PWA/service-worker cache
   name need bumping in `PwaController` so users pick it up?
