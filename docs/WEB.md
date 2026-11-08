# WEB — Thymeleaf website (reverse-engineered 2026-11-08)

Templates: `src/main/resources/templates/`. Shared fragments in `templates/fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). `NavModelAdvice`/`SiteModelAdvice` add global model attributes; `GlobalExceptionHandler` renders `error.html`.

| Area | Routes | Controller | Services |
|---|---|---|---|
| Marketing/public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | PricingController, DemoController, LegalController, SeoController | DemoConversationService |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | PwaController | — |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController | UserService, PasswordResetService, EmailVerificationService |
| Chats workspace | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | ConversationListService, ChatService, ReplyService, OutboundMessageService, MailboxPollingService |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|signature|delete`, `/mailboxes/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController | MailAccountService |
| Saved searches | `/searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| Account | `/account`, `/account/{notifications,appearance,privacy}`, `/account/password`, `/account/email` | AccountController | AccountService |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | BillingController, BillingWebhookController | BillingService |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| Digest | `/digest/opt-out` | DigestController | WeeklyDigestService |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | *MetricsService classes |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to `/chats` | ChatRedirectController | — |
