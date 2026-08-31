# Website (Thymeleaf)

Inventory of the server-rendered pages. Every page's controller must call
the same service layer the (planned) JSON API will call — no business
logic in the view layer.

Templates live in `src/main/resources/templates/` and share the layout
fragments in `templates/fragments/`.

## Marketing + auth

| Path | Controller | Template | Purpose |
|------|------------|----------|---------|
| `/` | `PricingController` | `landing.html` | Marketing landing |
| `/pricing` | `PricingController` | `pricing.html` | Plans, checkout entry |
| `/login` | `AuthController` | `login.html` | Form + Google login |
| `/register` (GET/POST) | `AuthController` | `register.html` | Sign-up |
| `/auth/google/start` | `OAuthStartController` | — (redirect) | Start Google OIDC |
| `/verify-email` | `EmailVerificationController` | `verify-email.html` | Confirm mailbox |
| `/verify-email/resend` (POST) | `EmailVerificationController` | — | Re-send verification |
| `/password/forgot` (GET/POST) | `PasswordResetController` | `password/*.html` | Reset flow |
| `/password/reset` (GET/POST) | `PasswordResetController` | `password/*.html` | Complete reset |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal.html` | Static legal |
| `/demo` | `DemoController` | — | Demo conversation |

## Chats workspace

| Path | Controller | Template | Purpose |
|------|------------|----------|---------|
| `/chats` | `ConversationController` | `chat.html` | Rail + open pane |
| `/chats/new` (GET/POST) | `ConversationController` | `compose.html` | Start conversation |
| `/chats/refresh` (POST) | `ConversationController` | — | Force poll now |
| `/chats/{key}` | `ConversationController` | `chat.html` | One conversation |
| `/chats/{key}/pane` | `ConversationController` | fragment | Pane-only refresh |
| `/chats/{key}/send` (POST) | `ConversationController` | — | Reply |
| `/chats/events` | `ChatEventsController` | — (SSE) | Live new-mail stream |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | — | Legacy → `/chats` |

## Account, mailboxes, teams

| Path | Controller | Template | Purpose |
|------|------------|----------|---------|
| `/account` | `AccountController` | `account.html` | Overview |
| `/account/notifications` | `AccountController` | `account-notifications.html` | Digest/live opts |
| `/account/appearance` (GET/POST) | `AccountController` | `account-appearance.html` | Theme, accents |
| `/account/privacy` | `AccountController` | `account-privacy.html` | Data controls |
| `/account/password` (POST) | `AccountController` | — | Change password |
| `/account/email` (POST) | `AccountController` | — | Change email |
| `/mailboxes/new` | `MailboxController` | `mailboxes/new.html` | Connect IMAP |
| `/mailboxes/{id}/sync` (POST) | `MailboxController` | — | Manual poll |
| `/mailboxes/preferences` (POST) | `MailboxController` | — | Polling policy |
| `/mailboxes/{id}/signature` (POST) | `MailboxController` | — | Set signature |
| `/mailboxes/{id}/delete` (POST) | `MailboxController` | — | Disconnect |
| `/mailboxes/gmail/connect` | `GmailConnectController` | — | Start Gmail OAuth |
| `/mailboxes/gmail/reconnect/{id}` | `GmailConnectController` | — | Re-consent |
| `/mailboxes/gmail/callback` | `GmailConnectController` | — | OAuth return |
| `/team/invite` (GET/POST) | `TeamInviteController` | `team/*.html` | Invite |
| `/team/invite/accept` (GET/POST) | `TeamInviteController` | `team/*.html` | Accept |
| `/searches` `/searches/{id}/delete` | `SavedSearchController` | fragments | Saved searches |
| `/digest/opt-out` | `DigestController` | — | One-click opt-out |

## Billing, admin, PWA/SEO

| Path | Controller | Purpose |
|------|------------|---------|
| `/billing/checkout`, `/billing/portal`, `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription` (GET/POST), `/billing/webhook` | `BillingController`, `BillingWebhookController` | Stripe |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period` (POST), `/admin/retention/win-back` (POST) | `AdminRevenueController` | Admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*.png`, `/apple-touch-icon.png` | `PwaController` | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | SEO |

## Shared fragments

- `fragments/layout.html` — page shell (nav, theme, PWA head)
- `fragments/nav.html` — top nav
- `fragments/live-refresh.html` — SSE glue for `/chats/events`
- `fragments/onboarding.html` — checklist + nudges

## Rule of thumb for changes

- New feature that both website and mobile need? Add the endpoint in the
  service layer once; have `ConversationController` (or peers) render the
  Thymeleaf view AND the JSON controller return the DTO. Never split.
