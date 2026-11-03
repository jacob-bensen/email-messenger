# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `app-nav`, `chat-pane`, `live-refresh` (SSE in-place morph),
`settings-nav`, `seo`, `cookie-banner`, `install-banner`, `sw-register`. Global model: `NavModelAdvice`, `SiteModelAdvice`;
errors: `GlobalExceptionHandler`.

| Routes | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing | DemoConversationService, plan catalog |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService, OAuth2ProvisioningService |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | EmailVerificationService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account*.html | AccountService |
| `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` | ConversationController, ChatEventsController | chat, compose | ConversationService, ReplyService, ChatEventStream |
| `/threads/**`, `/senders` | ChatRedirectController | — (redirect to /chats) | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | MailboxController | mailboxes/* | MailAccountService, MailboxPollingService |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GmailConnectController | — | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (form on chat) | repository |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | team services |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | billing services |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/revenue | admin services |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | — |
| `/digest/opt-out` | DigestController | digest/opt-out | digest services |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | — |
