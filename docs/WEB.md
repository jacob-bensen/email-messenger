# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `templates/fragments` (chat pane, live-refresh, nav). Model attributes common to all pages are added by `NavModelAdvice` and `SiteModelAdvice`. Errors render `error.html` via `GlobalExceptionHandler`.

| Route | Auth | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | public | landing, pricing | `PlanLimitService`, `LandingProperties` |
| `/demo` | public | conversation | `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | public | login, register | `UserService`, OAuth classes |
| `/password/forgot`, `/password/reset` | public | password/* | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | public / auth | verify-email | `EmailVerificationService` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send` | auth | chat, compose, fragments/chat-pane | `ConversationListService`, `ChatService`, `OutboundMessageService`, `MailboxPollingService` |
| `/chats/refresh` (JSON), `/chats/events` (SSE) | auth | – | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | auth | redirect to /chats | – |
| `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/*` | auth | mailboxes/* | `MailAccountService`, `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | auth | (POST only) | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | auth | account* | `AccountService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | auth (`/billing/cancel` public) | billing/* | `BillingService` |
| `/team/invite`, `/team/invite/accept` | auth | team/* | `TeamInviteService` |
| `/digest/opt-out` | public | digest/* | `DigestController` |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | operator | admin/revenue | `*MetricsService` |
| `/privacy`, `/terms`, `/refund` | public | legal | `LegalProperties` |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | – | SEO/PWA controllers |
