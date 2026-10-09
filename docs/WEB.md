# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing`, `/demo` | `landing`, `pricing`, `chat` (demo) | `PricingController`, `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | `login`, `register` | `AuthController`, `OAuthStartController` → `UserService` |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` → `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `verify-email` | `EmailVerificationController` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `chat`, `compose`, `fragments/chat-pane` | `ConversationController`, `ChatEventsController` → `ChatService`, `ConversationListService`, `OutboundMessageService`, `ReplyService`, `MailboxPollingService` |
| `/threads/**`, `/senders` | — | `ChatRedirectController` (redirects to `/chats`) |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches/**` | (fragment on chats) | `SavedSearchController` → `SavedSearchService` |
| `/account/**` | `account*` | `AccountController` → `AccountService` |
| `/billing/**` | `billing/*` | `BillingController` → `BillingService` |
| `/team/invite[/accept]` | `team/*` | `TeamInviteController` → `TeamInviteService` |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` |
| `/admin/revenue` | `admin/revenue` | `AdminRevenueController` (allow-listed) |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | — | `SeoController`, `PwaController` |
