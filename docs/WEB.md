# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (app-nav,
chat-pane, live-refresh, settings-nav, seo, cookie/install banners, sw-register).

| Route | Auth | Controller | Template |
|---|---|---|---|
| `/`, `/pricing` | public | `PricingController` | `landing`, `pricing` |
| `/demo` | public | `DemoController` | (demo conversation) |
| `/privacy` `/terms` `/refund` | public | `LegalController` | `legal` |
| `/login` `/register` `/auth/google/start` | public | `AuthController`, `OAuthStartController` | `login`, `register` |
| `/password/forgot` `/password/reset` | public | `PasswordResetController` | `password/*` |
| `/verify-email` (+`/resend`) | public / auth | `EmailVerificationController` | `verify-email` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/refresh`, `/chats/{key}/send` | auth | `ConversationController` | `chat`, `compose`, `fragments/chat-pane` |
| `/chats/events` (SSE) | auth | `ChatEventsController` | — |
| `/threads/**`, `/senders` | auth | `ChatRedirectController` (legacy redirect) | — |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | auth | `MailboxController` | `mailboxes/*` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | auth | `GmailConnectController` | — |
| `/searches`, `/searches/{id}/delete` | auth | `SavedSearchController` | — |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | auth | `AccountController` | `account*` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | auth (cancel public) | `BillingController` | `billing/*` |
| `/billing/webhook` | public, Stripe-signed | `BillingWebhookController` | — |
| `/team/invite`, `/team/invite/accept` | auth | `TeamInviteController` | `team/*` |
| `/digest/opt-out` | public | `DigestController` | `digest/opt-out` |
| `/admin/revenue` (+ POST actions) | admin | `AdminRevenueController` | `admin/revenue` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | public | `PwaController` | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | — |
