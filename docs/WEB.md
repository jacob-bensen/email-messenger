# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route | Controller | Template | Services |
|---|---|---|---|
| `GET /`, `/pricing` | PricingController | landing, pricing | BillingProperties, SiteProperties |
| `GET /demo` | DemoController | chat (demo) | DemoConversationService |
| `/login`, `/register`, `/logout` | AuthController | login, register | UserService |
| `/auth/google/start` | OAuthStartController | — | OAuthIntentStore |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | EmailVerificationService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send` | ConversationController | chat, compose, fragments/chat-pane | ConversationListService, ChatService, ReplyService, OutboundMessageService |
| `GET /chats/events` (SSE) | ChatEventsController | — | ChatEventStream |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | MailboxController | mailboxes/* | MailAccountService, MailboxPollingService |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GmailConnectController | — | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | — | SavedSearchService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite[/accept]` | TeamInviteController | team/* | TeamInviteService |
| `/digest/opt-out` | DigestController | digest/* | WeeklyDigestService |
| `/admin/revenue` + 2 POSTs | AdminRevenueController | admin/* | *MetricsService |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | LegalProperties |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | — |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | — |
