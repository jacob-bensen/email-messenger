# Website (Thymeleaf) inventory

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | LandingProperties |
| `/demo` | DemoController | conversation | DemoConversationService |
| `/login`, `/register` | AuthController | login, register | UserService |
| `/auth/google/start` | OAuthStartController | — | OAuthIntentStore |
| `/password/forgot|reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | EmailVerificationService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `/chats` (list, `?q&mailbox&filter&page`) | ConversationController | chat | ConversationListService, MailboxPollingService |
| `/chats/new` GET/POST | ConversationController | compose | ReplyService, OutboundMessageService |
| `/chats/{key}` , `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | ChatService (marks read) |
| `/chats/{key}/send` POST | ConversationController | chat | ReplyService, OutboundMessageService (logic partly inline in controller) |
| `/chats/refresh` POST, `/chats/events` SSE | ConversationController, ChatEventsController | — | MailboxPollingService, ChatEventStream |
| `/mailboxes/**` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, PlanLimitService |
| `/searches` | SavedSearchController | — | SavedSearchService |
| `/billing/*` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite*` | TeamInviteController | team/* | TeamInviteService |
| `/admin/**` | AdminRevenueController | admin/* | *MetricsService |
| `/digest/opt-out` | DigestController | digest/* | WeeklyDigestService |
| `/privacy /terms /refund` | LegalController | legal | — |
| `/manifest.webmanifest /sw.js /offline /icons/**` | PwaController | offline | — |
| `/robots.txt /sitemap.xml /images/og-card.png` | SeoController | — | — |
| `/threads/**`, `/senders` | ChatRedirectController | — | redirects to `/chats` |
