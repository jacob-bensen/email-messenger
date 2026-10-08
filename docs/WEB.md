# WEB (Thymeleaf site)

Templates: `src/main/resources/templates`. Fragments in `templates/fragments`
(incl. `live-refresh.html` for SSE-driven in-place updates).

| Area | Routes | Controller |
|---|---|---|
| Landing/marketing | `/`, `/demo`, `/pricing`, `/terms`, `/privacy`, `/refund`, `/offline` | `web.*`, `LegalController`, `PricingController`, `DemoController` |
| Auth | `/login`, `/register`, `/password/forgot|reset`, `/verify-email[/resend]`, `/auth/google/start` | `auth.*` |
| Chats workspace | `/chats`, `/chats/new` (GET/POST), `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE); legacy `/threads/**`, `/senders` redirect | `ConversationController`, `ChatRedirectController`, `ChatEventsController` |
| Mailboxes | `/mailboxes/**` (connect, `{id}/sync`, `{id}/delete`, `{id}/signature`), `/mailboxes/gmail/**` (connect, callback, reconnect) | `MailboxController`, `GmailConnectController` |
| Saved searches | `/searches/**` | `SavedSearchController` |
| Account | `/account`, `/account/{appearance,notifications,privacy,email,password}` | `AccountController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `billing.*` |
| Teams | `/team/invite[/accept]` | `TeamInviteController` |
| Digest | `/digest/opt-out` | `DigestController` |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `AdminRevenueController` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, icons, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` |

Pages call services (`ChatService`, `ReplyService`, `MailAccountService`,
`BillingService`, ...); services are the shared layer any future API must reuse.
