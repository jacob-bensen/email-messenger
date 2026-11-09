# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | PlanLimits/pricing props |
| `/demo` | DemoController | chat (demo) | DemoConversationService |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService, OAuth2ProvisioningService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | EmailVerificationService |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | ConversationListService, ChatService, ReplyService, OutboundMessageService, MailboxPollingService, ChatEventStream |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` | - |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (POST + redirect) | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | TeamInviteService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/* | *MetricsService, WinBackOutreachService |
| `/digest/opt-out` | DigestController | digest/* | WeeklyDigestService |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | - |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | - | - |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | PwaController | - | - |
