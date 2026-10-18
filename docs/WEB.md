# Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | `auth.*Controller` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` (fragment), `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` |
| Legacy redirects | `/threads/**`, `/senders` → chats | `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Digest | `/digest/opt-out` | `DigestController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` |
