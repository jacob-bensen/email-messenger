# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `app-nav`, `chat-pane`, `cookie-banner`,
`install-banner`, `live-refresh`, `seo`, `settings-nav`, `sw-register`.

| Routes | Controller | Template(s) | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | `landing`, `pricing` | `DemoConversationService` |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal` | – |
| `/login` `/register` | `AuthController` | `login`, `register` | `UserService` |
| `/password/forgot|reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email[/resend]` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `conversation`, `fragments/chat-pane` | `ConversationService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` | `mailboxes/*` | `MailAccountService` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | – (redirects) | `SavedSearchService` |
| `/account/*` | `AccountController` | `account*` | `AccountService` |
| `/billing/*` | `BillingController` | `billing/*` | `BillingService` |
| `/team/invite[/accept]` | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | admin metric services |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` | – |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | – | – |
