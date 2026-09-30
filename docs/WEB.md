# Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments`.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/demo`, `/pricing`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | landing, pricing, legal | `DemoConversationService`, `PlanPricing` |
| `/login`, `/register`, `/password/*`, `/verify-email` | `AuthController`, `PasswordResetController`, `EmailVerificationController` | login, register, password/*, verify-email | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | chat, compose, fragments/chat-pane | `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | (redirects to `/chats`) | — |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` | mailboxes/* | `MailAccountService`, `GmailOAuthClient`, `PlanLimitService` |
| `/searches/**` | `SavedSearchController` | (posts, redirects) | `SavedSearchService` |
| `/account/**` | `AccountController` | account*, | `AccountService` |
| `/billing/**` | `BillingController`, `BillingWebhookController` | billing/* | `BillingService`, Stripe gateways |
| `/team/invite*` | `TeamInviteController` | team/* | `TeamInviteService` |
| `/admin/**` | `AdminRevenueController` | admin/revenue | `*MetricsService` |
| `/digest/opt-out` | `DigestController` | digest/opt-out | `WeeklyDigestService` |
| PWA/SEO | `PwaController`, `SeoController` | — | manifest, `/sw.js`, `/offline`, robots, sitemap |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE + in-place morph),
`cookie-banner`, `install-banner`, `seo`, `sw-register`.

Note: `ConversationController` contains orchestration logic (mailbox selection, send-as-reply vs new email)
that a REST API would need to reuse; it should be extracted into a service first (see `INTERNAL_TODO.md`).
