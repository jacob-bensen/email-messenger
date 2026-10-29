# Thymeleaf website (route inventory, from controller annotations)

| Area | Routes | Controller |
|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | PricingController, DemoController, LegalController, SeoController |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `POST /verify-email/resend` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh` (JSON), `/chats/events` (SSE); legacy `/threads/**`,`/senders` redirect | ConversationController, ChatEventsController, ChatRedirectController |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Searches | `POST /searches`, `POST /searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{appearance,password,email}` | AccountController |
| Billing | `POST /billing/{checkout,portal,cancel-subscription}`, `/billing/{success,cancel,cancel-subscription}`, `POST /billing/webhook` | BillingController, BillingWebhookController |
| Team | `/team/invite`, `/team/invite/accept` (GET+POST) | TeamInviteController |
| Digest | `/digest/opt-out` | DigestController |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | AdminRevenueController |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | PwaController |

Templates: `src/main/resources/templates` (shared bits in `fragments/`, notably `live-refresh.html`). Model attributes common to all pages come from `NavModelAdvice` / `SiteModelAdvice`.
