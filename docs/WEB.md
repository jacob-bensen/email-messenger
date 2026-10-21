# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing | DemoConversationService |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService |
| `/password/forgot\|reset`, `/verify-email[/resend]` | PasswordReset-/EmailVerificationController | password/*, verify-email | PasswordResetService, EmailVerificationService |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | ConversationListService, ChatService, OutboundMessageService, MailboxPollingService |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` | — |
| `/mailboxes` (+`/new`, `/{id}/sync\|signature\|delete`, `/preferences`), `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, MailboxPollingService |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (posts from chat) | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-*.html | AccountService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | billing services |
| `/team/invite[/accept]` | TeamInviteController | team/* | team services |
| `/admin/revenue` (+POSTs) | AdminRevenueController | admin/* | admin services |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | — |
| `/digest/opt-out` | DigestController | digest/* | digest service |
