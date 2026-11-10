# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | BillingProperties/PlanPricing |
| `/demo` | DemoController | chat (demo data) | DemoConversationService |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService, OAuth2ProvisioningService |
| `/verify-email[/resend]` | EmailVerificationController | verify-email | EmailVerificationService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh` | ConversationController | chat, compose, fragments/chat-pane | ChatService, ConversationListService, MailAccountService, OutboundMessageService, MailboxPollingService |
| `/chats/events` (SSE) | ChatEventsController | – | ChatEventStream |
| `/mailboxes`, `/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences`, `/gmail/*` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches` (+`/{id}/delete`) | SavedSearchController | (embedded in chat) | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `POST /billing/webhook` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite[/accept]` | TeamInviteController | team/* | TeamInviteService |
| `/digest/opt-out` | DigestController | digest/* | WeeklyDigestService |
| `/admin/revenue` + POST actions | AdminRevenueController | admin/* | *MetricsService |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | LegalProperties |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | – | – |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | – | SiteProperties |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` | – |
