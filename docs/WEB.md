# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments in `fragments/`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Route(s) | Controller | Template | Main services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, conversation | DemoConversationService |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email`(+`/resend`) | EmailVerificationController | verify-email | EmailVerificationService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` | ConversationController, ChatEventsController | chat, compose | ConversationListService, ChatService, OutboundMessageService, MailboxPollingService |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | — |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController | mailboxes/* | MailAccountService |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GmailConnectController | — | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | — | SavedSearchService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | TeamInviteService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | BillingService |
| `/digest/opt-out` | DigestController | digest/opt-out | WeeklyDigestService |
| `/admin/revenue` (+POST reconcile, win-back) | AdminRevenueController | admin/revenue | admin metrics services |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png`, PWA assets | SeoController, PwaController | — | — |
