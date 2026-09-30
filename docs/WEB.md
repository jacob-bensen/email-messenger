# Thymeleaf website

Templates: `src/main/resources/templates/`. Shared fragments in
`templates/fragments/` (`app-nav`, `chat-pane`, `settings-nav`, `live-refresh`,
`cookie-banner`, `install-banner`, `sw-register`, `seo`).

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing`, `/demo` | landing, pricing, chat (demo) | `PricingController`, `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/verify-email` | login, register, verify-email | `AuthController`, `EmailVerificationController` → `UserService` |
| `/password/forgot`, `/password/reset` | password/* | `PasswordResetController` → `PasswordResetService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | chat, compose, fragments/chat-pane | `ConversationController` → `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService` |
| `/threads/**`, `/senders` | — | `ChatRedirectController` (redirects to /chats) |
| `/mailboxes/**`, `/mailboxes/gmail/**` | mailboxes/* | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/account/**` | account*.html | `AccountController` → `AccountService` |
| `/billing/**` | billing/* | `BillingController` → `BillingService` |
| `/team/invite/**` | team/* | `TeamInviteController` |
| `/searches/**` | (in chat) | `SavedSearchController` → `SavedSearchService` |
| `/digest/opt-out` | digest/* | `DigestController` |
| `/admin/**` | admin/* | `AdminRevenueController` → admin metrics services |
| `/privacy`, `/terms`, `/refund` | legal | `LegalController` |
| `/robots.txt`, `/sitemap.xml`, PWA assets | — | `SeoController`, `PwaController` |
