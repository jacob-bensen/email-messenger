# WEB.md — Thymeleaf website (reverse-engineered 2026-10-12)

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Model attributes common to pages are added by
`NavModelAdvice` / `SiteModelAdvice`; errors by `GlobalExceptionHandler`.

| Area | Routes | Controller → services |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController` (`DemoConversationService`), `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/verify-email`(+`/resend`), `/password/forgot`, `/password/reset` | `AuthController`, `OAuthStartController`, `EmailVerificationController`, `PasswordResetController` |
| Chats (core) | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`; `GET /chats/events` (SSE) | `ConversationController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` → `AccountService` |
| Billing | `POST /billing/checkout`, `/billing/portal`, `GET /billing/success`, `/billing/cancel`, `/billing/cancel-subscription`, `POST /billing/webhook` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` (gated by `admin.emails`) |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` |
