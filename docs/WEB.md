# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Shared fragments: `fragments/{app-nav,chat-pane,live-refresh,settings-nav,seo,cookie-banner,install-banner,sw-register}.html`.

| Area | Routes | Controller | Services used |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `DemoConversationService` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/*`, `/verify-email*` | `auth.*Controller` | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats workspace | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `MailboxPollingService` |
| Saved searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `BillingService` |
| Team | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team services |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | admin services |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` | — |
