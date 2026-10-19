# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Shared bits in `templates/fragments`
(e.g. `live-refresh.html` wires SSE `/chats/events` to in-place rail/pane
updates in `chat.html`).

| Area | Routes | Controller | Services |
|---|---|---|---|
| Landing/legal/SEO | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` | `DemoConversationService` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` | `UserService`, `EmailVerificationService`, `PasswordResetService` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `ConversationListService`, `ChatService`, `OutboundMessageService`, `ReplyService` |
| Legacy | `/threads/**`, `/senders` | `ChatRedirectController` | redirects to /chats |
| Mailboxes | `/mailboxes`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` | `MailAccountService` |
| Searches | `/searches` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/appearance|notifications|privacy|email|password` | `AccountController` | `AccountService` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` | `BillingService` |
| Team | `/team/invite[/accept]` | `TeamInviteController` | `TeamInviteService` |
| Digest | `/digest/opt-out`, `/preferences` | `DigestController` | `WeeklyDigestService` |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `AdminRevenueController` | `*MetricsService` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` | — |
