# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (app-nav, chat-pane,
live-refresh, settings-nav, seo, cookie/install banners, sw-register).

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | PricingController, DemoController, LegalController | landing, pricing, legal |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot\|reset`, `/verify-email[/resend]` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController | login, register, password/*, verify-email |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync\|signature\|delete`, `/preferences`, `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | MailboxController, GmailConnectController | mailboxes/* |
| Searches | `POST /searches`, `/searches/{id}/delete` | SavedSearchController | (in chat) |
| Account | `/account`, `/account/notifications\|appearance\|privacy`, `POST /account/password\|email` | AccountController | account*.html |
| Billing | `/billing/checkout\|portal\|success\|cancel\|cancel-subscription` | BillingController | billing/* |
| Team | `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* |
| Admin | `/admin/revenue` (+ reconcile, win-back POSTs) | AdminRevenueController | admin/revenue |
| Misc | `/digest/opt-out`, `/threads*` (redirect to chats), PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons), SEO (`/robots.txt`, `/sitemap.xml`) | DigestController, ChatRedirectController, PwaController, SeoController | digest/opt-out |
