# Website (Thymeleaf)

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `GET /`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email[/resend]`, `/password/forgot`, `/password/reset` | `AuthController`, `OAuthStartController`, `EmailVerificationController`, `PasswordResetController` | `login`, `register`, `verify-email`, `password/*` |
| Chats workspace | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send`; `GET /chats/events` (SSE); legacy `/threads/**`, `/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `chat`, `compose`, `fragments/chat-pane` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `POST /mailboxes[/{id}/sync\|/preferences\|/{id}/signature\|/{id}/delete]`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | (chat rail) |
| Account | `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{appearance,password,email}` | `AccountController` | `account*` |
| Billing | `POST /billing/{checkout,portal,cancel-subscription}`, `GET /billing/{success,cancel,cancel-subscription}`, `POST /billing/webhook` | `BillingController`, `BillingWebhookController` | `billing/*` |
| Teams | `/team/invite`, `/team/invite/accept` (GET+POST) | `TeamInviteController` | `team/*` |
| Digest | `GET /digest/opt-out` | `DigestController` | `digest/*` |
| Admin | `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/*` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | `offline` |

Model advice: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler` → `error`.
