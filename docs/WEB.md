# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`, `settings-nav`,
`chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | `landing`, `pricing` | `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` | `UserService` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` | `AccountService` |
| `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send` | `ConversationController` | `chat`, `compose`, `fragments/chat-pane` | `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/chats/events` (SSE) | `ChatEventsController` | — | `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `mailboxes/*` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | redirects | `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (posts from chat) | `SavedSearchService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` | `WeeklyDigestService` |
| `/admin/revenue` (+POSTs) | `AdminRevenueController` | `admin/revenue` | `*MetricsService` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` | — |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — | — |
