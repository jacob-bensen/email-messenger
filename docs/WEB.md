# WEB (Thymeleaf)

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `team/`, `mailboxes/`, `digest/`, `billing/`, `password/`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | PricingController, DemoController, LegalController |
| Auth | `/login`, `/register`, `/verify-email[/resend]`, `/password/forgot`, `/password/reset`, `/auth/google/start` | AuthController, EmailVerificationController, PasswordResetController, OAuthStartController |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Saved searches | `/searches`, `/searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController, BillingWebhookController |
| Team | `/team/invite[/accept]` | TeamInviteController |
| Digest | `/digest/opt-out` | DigestController |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | ChatRedirectController |
