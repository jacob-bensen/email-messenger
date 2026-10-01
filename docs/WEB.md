# Thymeleaf website

Templates: `src/main/resources/templates/`; shared fragments in `fragments/`
(incl. `live-refresh.html` for SSE-driven in-place updates).

| Area | Routes | Controller | Template |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot\|reset`, `/verify-email` | `auth/*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync\|signature\|delete`, `/preferences`, `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (fragment in chat) |
| Account | `/account`, `/account/notifications\|appearance\|privacy\|password\|email` | `AccountController` | `account*` |
| Billing | `/billing/checkout\|portal\|success\|cancel\|cancel-subscription` | `BillingController` | `billing/*` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| Admin | `/admin/revenue`, `.../reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/*` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` | — |
