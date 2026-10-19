# Website (Thymeleaf)

Templates: `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email[/resend]`, `/password/forgot`, `/password/reset` | `auth.*Controller` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; `/threads`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `billing.*Controller` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` |
| Digest | `/digest/opt-out` | `DigestController` |
