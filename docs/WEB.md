# WEB.md — Thymeleaf website

Templates live in `src/main/resources/templates`; shared pieces in `templates/fragments/`
(e.g. `chat-pane`, `live-refresh`). Model attributes common to pages come from `NavModelAdvice` / `SiteModelAdvice`.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | `landing`, `pricing` | `DemoConversationService`, `BillingProperties` |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` | `UserService`, `OAuth2ProvisioningService` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` | `AccountService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` | `ConversationListService`, `ChatService`, `OutboundMessageService`, `MailboxPollingService`, `ChatEventStream` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` | `MailAccountService`, `GmailOAuthClient`, `PlanLimitService` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (redirects) | `SavedSearchService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | team services |
| `/digest/opt-out` | `DigestController` | `digest/*` | digest services |
| `/admin/revenue`, `/admin/...` | `AdminRevenueController` | `admin/*` | revenue services |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` | `LegalProperties` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — | — |
| `/threads`, `/senders` | `ChatRedirectController` | — | legacy redirect to `/chats` |
