# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments`.
Global model attributes come from `NavModelAdvice` and `SiteModelAdvice`; errors via `GlobalExceptionHandler`.

| Route(s) | Controller | Template(s) | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | landing, pricing, chat (demo) | `DemoConversationService`, `PlanLimitService` |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | login, register | `UserService`, `OAuth2ProvisioningService` |
| `/password/forgot|reset`, `/verify-email[/resend]` | `PasswordResetController`, `EmailVerificationController` | password/*, verify-email | `PasswordResetService`, `EmailVerificationService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | chat, compose, fragments/chat-pane | `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService`, `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | (redirect to /chats) | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/mailboxes/preferences` | `MailboxController` | mailboxes/* | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | — | `GmailOAuthClient`, `MailAccountService` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (redirects) | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | account*.html | `AccountService`, `BillingService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` | billing/* | `BillingService`, Stripe gateways |
| `/team/invite[/accept]` | `TeamInviteController` | team/* | team services |
| `/digest/opt-out` | `DigestController` | digest/* | `WeeklyDigestService` |
| `/admin/revenue` (+ reconcile, win-back POSTs) | `AdminRevenueController` | admin/revenue | `*MetricsService` (operator only via `AdminAuthorizer`) |
| `/privacy`, `/terms`, `/refund` | `LegalController` | legal | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | offline | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — | — |

