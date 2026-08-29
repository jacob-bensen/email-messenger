# WEB.md — Thymeleaf website inventory

The backend also serves the public marketing site, the authenticated web
app, legal pages and the PWA shell. Every route below must call the same
service layer that the mobile API in `docs/API.md` uses.

## Marketing / public

| Route | Controller | Template | Notes |
|-------|------------|----------|-------|
| `GET /` | `PricingController` | `landing.html` | Landing hero + video. |
| `GET /pricing` | `PricingController` | `pricing.html` | Plan cards. |
| `GET /demo` | `DemoController` | (redirects) | Demo entry point. |
| `GET /privacy` | `LegalController` | `legal.html` | Privacy policy. |
| `GET /terms` | `LegalController` | `legal.html` | Terms of service. |
| `GET /refund` | `LegalController` | `legal.html` | Refund policy. |
| `GET /robots.txt` | `SeoController` | — | Static text. |
| `GET /sitemap.xml` | `SeoController` | — | Sitemap. |
| `GET /images/og-card.png` | `SeoController` | — | Open Graph card. |

## Auth

| Route | Controller |
|-------|------------|
| `GET/POST /login` | `AuthController` |
| `GET/POST /register` | `AuthController` |
| `GET /auth/google/start` | `OAuthStartController` |
| `GET/POST /password/forgot` | `PasswordResetController` |
| `GET/POST /password/reset` | `PasswordResetController` |
| `GET /verify-email` | `EmailVerificationController` |
| `POST /verify-email/resend` | `EmailVerificationController` |

## Account settings

| Route | Controller | Template |
|-------|------------|----------|
| `GET /account` | `AccountController` | `account.html` |
| `GET /account/notifications` | `AccountController` | `account-notifications.html` |
| `GET/POST /account/appearance` | `AccountController` | `account-appearance.html` |
| `GET /account/privacy` | `AccountController` | `account-privacy.html` |
| `POST /account/password` | `AccountController` | — |
| `POST /account/email` | `AccountController` | — |

## Chats workspace (the product)

| Route | Controller | Notes |
|-------|------------|-------|
| `GET /chats` | `ConversationController` | Rail + open pane. |
| `GET/POST /chats/new` | `ConversationController` | Compose. |
| `POST /chats/refresh` | `ConversationController` | JSON, kicks polling. |
| `GET /chats/{key}` | `ConversationController` | Full page for one chat. |
| `GET /chats/{key}/pane` | `ConversationController` | HTMX-style fragment. |
| `POST /chats/{key}/send` | `ConversationController` | Reply send. |
| `GET /chats/events` | `ChatEventsController` | SSE stream. |
| `GET /threads`, `/threads/**`, `/senders` | `ChatRedirectController` | Legacy → `/chats`. |

## Mailboxes

| Route | Controller |
|-------|------------|
| `GET /mailboxes` | `MailboxController` |
| `GET /mailboxes/new` | `MailboxController` |
| `POST /mailboxes` | `MailboxController` |
| `POST /mailboxes/{id}/sync` | `MailboxController` |
| `POST /mailboxes/preferences` | `MailboxController` |
| `POST /mailboxes/{id}/signature` | `MailboxController` |
| `POST /mailboxes/{id}/delete` | `MailboxController` |
| `GET /mailboxes/gmail/connect` | `GmailConnectController` |
| `GET /mailboxes/gmail/reconnect/{id}` | `GmailConnectController` |
| `GET /mailboxes/gmail/callback` | `GmailConnectController` |

## Billing (Stripe)

| Route | Controller |
|-------|------------|
| `POST /billing/checkout` | `BillingController` |
| `POST /billing/portal` | `BillingController` |
| `GET /billing/success` | `BillingController` |
| `GET /billing/cancel` | `BillingController` |
| `GET/POST /billing/cancel-subscription` | `BillingController` |
| `POST /billing/webhook` | `BillingWebhookController` |

## Team

| Route | Controller |
|-------|------------|
| `GET/POST /team/invite` | `TeamInviteController` |
| `GET/POST /team/invite/accept` | `TeamInviteController` |

## Saved searches

| Route | Controller |
|-------|------------|
| `POST /saved-searches` | `SavedSearchController` |
| `POST /saved-searches/{id}/delete` | `SavedSearchController` |

## Digest

| Route | Controller |
|-------|------------|
| `GET /digest/opt-out` | `DigestController` |

## Admin

| Route | Controller |
|-------|------------|
| `GET /admin/revenue` | `AdminRevenueController` |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController` |
| `POST /admin/retention/win-back` | `AdminRevenueController` |

## PWA

`PwaController` serves `manifest.webmanifest`, icon PNGs, `sw.js`
(service worker), and `offline` page. Web Push (VAPID keys, subscription
storage) is not wired yet — see the roadmap in the root `CLAUDE.md`.

## Shared fragments / layouts

`templates/fragments/` holds header, footer, chat rail, chat pane,
compose form, live-refresh SSE binding. Anything a page shares — nav,
plan banner, onboarding nudges — should live here.

## Business logic ↔ presentation rule

Controllers stay thin. All mutation must go through the service classes
(`ConversationService`, `ReplyService`, `MailboxService`, `BillingService`,
`AuthService`, etc.). When the mobile REST API lands, its controllers
will call the same services — no forked logic between the two paths.
