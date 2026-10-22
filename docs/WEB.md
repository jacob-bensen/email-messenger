# WEB.md — Thymeleaf website

Templates live in `src/main/resources/templates`; shared pieces in `fragments/`.

| Route | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | PlanLimits, SiteProperties |
| `/demo` | DemoController | chat (demo data) | DemoConversationService |
| `/login`, `/register` | AuthController | login, register | UserService |
| `/password/forgot\|reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email[/resend]` | EmailVerificationController | verify-email | EmailVerificationService |
| `/auth/google/start` | OAuthStartController | — (redirect) | OAuthIntentStore |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send` | ConversationController | chat, compose, fragments/chat-pane | ChatService, ConversationListService, OutboundMessageService, MailboxPollingService |
| `/chats/refresh` (JSON), `/chats/events` (SSE) | ConversationController, ChatEventsController | — | MailboxPollingService, ChatEventStream |
| `/threads/**`, `/senders` | ChatRedirectController | — (redirect to /chats) | — |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, PlanLimitService |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | — | SavedSearchService |
| `/account[/notifications\|appearance\|privacy\|password\|email]` | AccountController | account-* | AccountService |
| `/billing/*` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite[/accept]` | TeamInviteController | team/* | TeamInviteService |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/* | metrics services |
| `/digest/opt-out` | DigestController | digest/* | digest services |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | LegalProperties |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | offline | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | — |
