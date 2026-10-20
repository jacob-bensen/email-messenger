# Website (Thymeleaf) — page inventory

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments` (app-nav, settings-nav, chat-pane, live-refresh, seo, cookie/install banners, sw-register).

| Area | Routes | Controller | Main services |
|---|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController` (also serves `/`)/`DemoController`/`LegalController`/`SeoController` | `DemoConversationService`, `SiteProperties` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|delete|signature`, `/mailboxes/gmail/connect|callback|reconnect/{id}` | `MailboxController`, `GmailConnectController` | `MailAccountService` |
| Saved searches | `/searches/**` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/{password,email,appearance,notifications,privacy}`, `/preferences` | `AccountController` | `AccountService` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `BillingService`, `PlanLimitService` |
| Team | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team services |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, … | `AdminRevenueController` | `*MetricsService` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` | — |
