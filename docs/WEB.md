# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/` (app-nav, chat-pane, live-refresh,
settings-nav, seo, cookie/install banners, sw-register). Model attributes common to pages come from `NavModelAdvice` / `SiteModelAdvice`.

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | PricingController, DemoController, LegalController | landing, pricing, conversation, legal |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController | login, register, password/*, verify-email |
| Chats (core) | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE); legacy `/threads/**`,`/senders` redirect | ConversationController, ChatEventsController, ChatRedirectController | chat, compose, fragments/chat-pane |
| Mailboxes | `/mailboxes` (+`/new`,`/{id}/sync`,`/{id}/signature`,`/{id}/delete`,`/preferences`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* |
| Saved searches | `POST /searches`, `POST /searches/{id}/delete` | SavedSearchController | (chat rail) |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController, BillingWebhookController | billing/* |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/* |
| Misc | `/digest/opt-out`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons), SEO (`/robots.txt`, `/sitemap.xml`, og-card) | DigestController, PwaController, SeoController | — |
