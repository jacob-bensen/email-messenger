# Thymeleaf website

Templates: `src/main/resources/templates/`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). Model-wide attributes via `NavModelAdvice` / `SiteModelAdvice`.

| Area | Routes | Controller | Services used |
|---|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | Landing/Pricing/Demo/Legal/Seo | `DemoConversationService`, `PlanLimits` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email`, `/password/forgot`, `/password/reset` | Auth*, PasswordReset, EmailVerification, OAuthStart | `UserService`, `EmailVerificationService`, `PasswordResetService` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads`, `/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `ConversationListService`, `ChatService`, `OutboundMessageService`, `ReplyService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `MailboxPollingService` |
| Saved searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `BillingService` |
| Teams / digest | `/team/invite[/accept]`, `/digest/opt-out` | `TeamInviteController`, `DigestController` | team + digest services |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `*MetricsService` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` | — |
