# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `chat-pane`, `settings-nav`, `live-refresh`, `seo`, `install-banner`,
`cookie-banner`, `sw-register`). `NavModelAdvice` / `SiteModelAdvice` add global model data.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, chat | DemoConversationService |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService |
| `/password/forgot|reset`, `/verify-email[/resend]` | PasswordReset/EmailVerificationController | password/*, verify-email | PasswordResetService, EmailVerificationService |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, conversation | ConversationListService, ConversationService, ReplyService, OutboundMessageService, MailboxPollingService |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (redirects) | SavedSearchRepository |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | BillingController | billing/* | BillingService |
| `/team/invite[/accept]` | TeamInviteController | team/* | TeamInviteService |
| `/admin/revenue`, `/admin/*` | AdminRevenueController | admin/* | admin metrics services |
| `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml`, PWA assets | LegalController, SeoController, PwaController | legal, … | — |
| `/digest/opt-out` | DigestController | digest/* | WeeklyDigestService |
