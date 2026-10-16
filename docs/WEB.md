# WEB (Thymeleaf site)

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`.
Controllers are package-private, return view names, and use the service layer listed.

| Route(s) | Controller | Auth | Services / notes |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | public | `landing.html`, `pricing.html`; `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | public | `UserService`, Google OIDC (only if `GOOGLE_CLIENT_ID` set) |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | token / session | `EmailVerificationService` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | public | `PasswordResetService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | session | `ConversationListService`, `ChatService`, `OutboundMessageService`, `MailboxPollingService`; `chat.html` + `fragments/chat-pane` |
| `/threads/**`, `/senders` | `ChatRedirectController` | session | legacy redirects to `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | session | `MailAccountService`, `GmailOAuthClient`, `PlanLimitService` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | session | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | session | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | session (`cancel` public) | `BillingService`, Stripe gateways |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | session | team services |
| `/digest/opt-out` | `DigestController` | public (token) | `WeeklyDigestService` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | public | |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `AdminAuthorizer` | `admin.*MetricsService`, `WinBackOutreachService` |
| PWA/SEO | `PwaController`, `SeoController` | public | |
Model-wide attributes come from `NavModelAdvice` / `SiteModelAdvice`; errors from `GlobalExceptionHandler`.
