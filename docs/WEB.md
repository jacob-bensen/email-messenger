# Thymeleaf website

Templates in `src/main/resources/templates`; shared parts in `fragments/` (e.g. `live-refresh.html`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `LandingController`(root), `PricingController`, `DemoController`, `LegalController` |
| Auth | `/login`, `/register`, `/verify-email`, `/password/forgot`, `/password/reset`, `/auth/google/start` | `auth.*Controller` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads/**`, `/senders` redirect | `ConversationController`, `ChatRedirectController`, `ChatEventsController` |
| Mailboxes | `/mailboxes/**`, `/mailboxes/gmail/**` (connect, reconnect/{id}, sync, delete, signature) | `MailboxController`, `GmailConnectController` |
| Account | `/account`, `/account/{appearance,notifications,privacy,email,password}` | `AccountController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` |
| Teams/searches/digest | `/team/invite[/accept]`, `/searches`, `/digest/opt-out`, `/preferences` | `TeamInviteController`, `SavedSearchController`, `DigestController` |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` |
