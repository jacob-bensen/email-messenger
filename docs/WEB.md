# Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing`, `/demo` | landing, pricing, conversation | `PricingController`, `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | login, register | `AuthController`, `OAuthStartController` → `UserService` |
| `/password/forgot`, `/password/reset` | password/* | `PasswordResetController` → `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | verify-email | `EmailVerificationController` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | chat, compose, fragments/chat-pane | `ConversationController`, `ChatEventsController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/threads/**`, `/senders` | — | `ChatRedirectController` (redirect to `/chats`) |
| `/mailboxes/**`, `/mailboxes/gmail/**` | mailboxes/* | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches` | — | `SavedSearchController` → `SavedSearchService` |
| `/account/**` | account*.html | `AccountController` → `AccountService` |
| `/billing/**` | billing/* | `BillingController` → `BillingService` |
| `/team/invite*` | team/* | `TeamInviteController` → `TeamInviteService` |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/admin/revenue`, `/admin/retention/*` | admin/revenue | `AdminRevenueController` → admin metric services |
| `/privacy`, `/terms`, `/refund` | legal | `LegalController` |
| `/robots.txt`, `/sitemap.xml`, PWA assets (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons) | — | `SeoController`, `PwaController` |
