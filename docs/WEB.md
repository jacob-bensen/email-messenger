# Thymeleaf website

Templates in `src/main/resources/templates`; static assets in `static/`.

| Area | Routes | Controller | Template |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/password/forgot|reset`, `/verify-email`, `/auth/google/start` | `auth.*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` (fragment), `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane`, `fragments/live-refresh` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/mailboxes/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (in chat rail) |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` | `account*` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController` | `billing/*` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `AdminRevenueController` | `admin/*` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |

Shared model attributes come from `NavModelAdvice` and `SiteModelAdvice`. Legacy `/threads/**`
and `/senders` redirect to `/chats` (`ChatRedirectController`).
