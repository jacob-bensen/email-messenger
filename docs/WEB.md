# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/{app-nav,chat-pane,live-refresh,seo,settings-nav,cookie-banner,install-banner,sw-register}`.
Global model attributes come from `web/NavModelAdvice` and `web/SiteModelAdvice`.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` (public) | PricingController, DemoController | landing, pricing, chat | DemoConversationService |
| `/login`, `/register`, `/password/forgot|reset`, `/verify-email` | auth/* | login, register, password/*, verify-email | UserService, PasswordResetService, EmailVerificationService |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | ConversationListService, ChatService, OutboundMessageService, MailboxPollingService |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (redirects) | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account-* | AccountService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `/billing/webhook` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | TeamInviteService |
| `/digest/opt-out` | DigestController | digest/opt-out | WeeklyDigestService |
| `/admin/revenue` (+2 POSTs) | AdminRevenueController | admin/revenue | admin metrics services |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | — |
