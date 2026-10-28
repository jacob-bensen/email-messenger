# WEB.md — Thymeleaf website (templates in `src/main/resources/templates`)

| Route(s) | Template | Controller | Notes |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `landing`, `pricing`, chat demo | `ConversationController`/`PricingController`/`DemoController` | public |
| `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `login`, `register`, `password/*`, `verify-email` | `auth.*Controller` | public (resend needs session) |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `chat`, `compose`, `fragments/chat-pane`, `fragments/live-refresh` | `ConversationController`, `ChatEventsController` | main workspace; `/threads`, `/senders` redirect via `ChatRedirectController` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController` | connect/sync/delete mailboxes |
| `/searches` | (redirect) | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `account*` | `AccountController` | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `billing/*` | `BillingController`, `BillingWebhookController` | Stripe |
| `/team/invite[/accept]` | `team/*` | `TeamInviteController` | |
| `/digest/opt-out` | `digest/*` | `DigestController` | public |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `admin/*` | `AdminRevenueController` | allowlisted operators, 404 otherwise |
| `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `legal` | `LegalController`, `SeoController` | public |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | — | `PwaController` | PWA shell |

Shared fragments live in `templates/fragments/`. Controllers resolve the user via `UserService.requireByEmail(principal)`
and delegate to services; no logic is forked into templates.
