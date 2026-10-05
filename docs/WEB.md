# Thymeleaf website

Templates: `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email`, `/password/forgot`, `/password/reset` | `auth/*` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` |
| Teams / searches / digest | `/team/invite[/accept]`, `/searches`, `/digest/opt-out` | `team/*`, `SavedSearchController`, `DigestController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` |
| Legacy | `/threads/**`, `/senders` redirect to `/chats` | `ChatRedirectController` |
