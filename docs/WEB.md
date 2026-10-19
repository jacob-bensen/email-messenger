# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Controllers call services directly;
there is no separate REST layer, so no web/API logic duplication exists today.

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/password/forgot\|reset`, `/verify-email`, `/auth/google/start` | `auth.*Controller` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE); legacy `/threads/**`, `/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/mailboxes/gmail/connect\|callback\|reconnect/{id}`, `/{id}/sync\|delete\|signature` | `MailboxController`, `GmailConnectController` |
| Saved searches | `/searches` | `SavedSearchController` |
| Account | `/account`, `/account/{appearance,notifications,privacy,email,password}`, `/preferences` | `AccountController` |
| Billing | `/billing/checkout\|portal\|success\|cancel\|cancel-subscription`, `/billing/webhook` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `AdminRevenueController` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` |

Shared fragments: `templates/fragments/` (incl. `live-refresh.html` for in-place chat updates).
