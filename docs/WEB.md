# Thymeleaf website

Templates: `src/main/resources/templates`; fragments in `templates/fragments`
(app-nav, chat-pane, cookie-banner, install-banner, live-refresh, seo,
settings-nav, sw-register).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | PricingController, DemoController, LegalController |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController |
| Chats | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE) | ConversationController, ChatEventsController |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|signature|delete`, `/mailboxes/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController |
| Saved searches | `/searches`, `/searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/appearance|notifications|privacy|password|email` | AccountController |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | BillingController, BillingWebhookController |
| Team | `/team/invite`, `/team/invite/accept` | TeamInviteController |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | AdminRevenueController |
| Misc | `/digest/opt-out`, legacy `/threads/**`, `/senders` (redirect to /chats), PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons), `/robots.txt`, `/sitemap.xml` | DigestController, ChatRedirectController, PwaController, SeoController |
