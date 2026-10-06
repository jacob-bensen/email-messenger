# WEB.md — Thymeleaf website (2026-10-06)

Templates: `src/main/resources/templates/`; shared fragments in `templates/fragments/`
(e.g. `live-refresh.html`). Static assets under `static/{css,js,images}`; PWA shell via `PwaController`.

| Area | Routes | Controller | Services |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | PricingController, DemoController, LegalController | DemoConversationService, PlanPricing |
| Chats | GET `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; POST `/chats/new`, `/chats/refresh` (JSON), `/chats/{key}/send`; SSE `/chats/events` | ConversationController, ChatEventsController | ConversationListService, ChatService, ReplyService, OutboundMessageService, MailboxPollingService |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to chats | ChatRedirectController | — |
| Mailboxes | `/mailboxes` (GET/POST), `/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`; Gmail `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | MailAccountService |
| Saved searches | POST `/searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| Auth/account | `/login`, `/register`, `/verify-email[/resend]`, `/password/{forgot,reset}`, `/auth/google/start`, `/account`, `/account/{notifications,appearance,privacy,password,email}` | auth.* controllers | UserService, AccountService, PasswordResetService |
| Billing | POST `/billing/{checkout,portal,webhook}`, `/billing/{success,cancel,cancel-subscription}` | BillingController, BillingWebhookController | BillingService |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| Digest | `/digest/opt-out` | DigestController | WeeklyDigestService |
| Admin | `/admin/revenue`, POST `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin.* metrics services |
| SEO/PWA | `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | SeoController, PwaController | — |
