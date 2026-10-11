# WEB — Thymeleaf site

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`:
`app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE + in-place morph), `seo`, `cookie-banner`,
`install-banner`, `sw-register`.

| Route(s) | Template | Controller | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | landing, pricing | PricingController, DemoController | LandingProperties, PlanPricing |
| `/privacy` `/terms` `/refund` | legal | LegalController | LegalProperties |
| `/login` `/register` `/password/*` `/verify-email` | login, register, password/*, verify-email | auth.* | UserService, PasswordResetService, EmailVerificationService |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send` | chat, compose, fragments/chat-pane | ConversationController | ChatService, ConversationListService, ReplyService, OutboundMessageService, MailAccountService |
| `/mailboxes/**` | mailboxes/* | MailboxController, GmailConnectController | MailAccountService, PlanLimitService |
| `/searches` | (posts back to /chats) | SavedSearchController | SavedSearchService |
| `/account/**` | account*.html | AccountController | AccountService |
| `/billing/**` | billing/* | BillingController | BillingService |
| `/team/invite*` | team/* | TeamInviteController | team services |
| `/admin/**` | admin/* | AdminRevenueController | admin metrics services |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | PwaController | — |
| `/robots.txt`, `/sitemap.xml` | — | SeoController | — |
| `/threads/**`, `/senders` | redirect to /chats | ChatRedirectController | — |
