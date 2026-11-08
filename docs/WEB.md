# WEB.md — Thymeleaf website (reverse-engineered 2026-11-08)

Templates: `src/main/resources/templates`. Shared fragments in `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model via `NavModelAdvice`, `SiteModelAdvice`; errors via `GlobalExceptionHandler` → `error.html`.

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `conversation`(demo), `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `auth.*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `chat`, `compose`, `fragments/chat-pane` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`; `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | (in chat) |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `billing/*` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| Digest | `/digest/opt-out` | `DigestController` | `digest/opt-out` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `PwaController`, `SeoController` | — |
