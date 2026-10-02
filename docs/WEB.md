# Thymeleaf website

Templates: `src/main/resources/templates/` (`chat.html`, `landing.html`,
`pricing.html`, `login/register`, `account*.html`, `mailboxes/`, `billing/`,
`team/`, `password/`, `digest/`, `admin/`, `legal.html`, `fragments/`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `auth.*Controller` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` |
| Saved searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite[/accept]` | `TeamInviteController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml`, `/digest/opt-out` | `PwaController`, `SeoController`, `DigestController` |
| Legacy | `/threads/**`, `/senders` redirect to `/chats` | `ChatRedirectController` |

Shared model advice: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler`.
