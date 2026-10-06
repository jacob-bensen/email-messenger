# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller → services |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `auth.*Controller` → `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE) | `ConversationController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService`, `ChatEventStream` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` → `SavedSearchService` |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` → `AccountService` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription`, `/billing/webhook` | `BillingController` → `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` → `TeamInviteService` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` → admin metrics services |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` |
| Other | `/digest/opt-out`, legacy `/threads/**`, `/senders` redirect to `/chats` | `DigestController`, `ChatRedirectController` |
