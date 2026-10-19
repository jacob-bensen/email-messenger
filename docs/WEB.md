# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`,
`cookie-banner`, `install-banner`, `sw-register`). `NavModelAdvice` and
`SiteModelAdvice` (`@ControllerAdvice`) add global model attributes;
`GlobalExceptionHandler` renders `error.html`.

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing` | `landing`, `pricing` | `PricingController` |
| `/demo` | (chat) | `DemoController` → `DemoConversationService` |
| `/login`, `/register` | `login`, `register` | `AuthController` → `UserService` |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` → `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `verify-email` | `EmailVerificationController` |
| `/auth/google/start` | redirect | `OAuthStartController` |
| `/chats` | `chat` | `ConversationController` → `ConversationListService`, `MailboxPollingService` |
| `/chats/new` | `compose` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `/chats/{key}` | `chat` | `ChatService`, `EmailThreadRepository` |
| `/chats/{key}/pane` | `fragments/chat-pane :: pane` | same |
| `/chats/{key}/send` | redirect | `ReplyService`, `OutboundMessageService` |
| `/chats/refresh`, `/chats/events` | JSON / SSE | see API.md |
| `/threads/**`, `/senders` | redirect to chats | `ChatRedirectController` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences` | `mailboxes/*` | `MailboxController` → `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | redirect | `GmailConnectController` → `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | redirect | `SavedSearchController` |
| `/account*` | `account*` | `AccountController` → `AccountService` |
| `/billing/*` | `billing/*` | `BillingController` → `BillingService` |
| `/team/invite[/accept]` | `team/*` | `TeamInviteController` |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` |
| `/admin/revenue`, `/admin/retention/win-back` | `admin/revenue` | `AdminRevenueController` → admin services |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SEO | `SeoController` |
