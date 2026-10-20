# WEB — Thymeleaf site inventory

Templates in `src/main/resources/templates`; shared fragments in `fragments/` (`app-nav`, `settings-nav`,
`chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).
Global model data: `NavModelAdvice`, `SiteModelAdvice`. Errors: `GlobalExceptionHandler` + `error.html`.

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/pricing`, `/demo` (public) | landing, pricing, conversation | `PricingController`, `DemoController` → `DemoConversationService` |
| `/login`, `/register` | login, register | `AuthController` → `UserService` |
| `/password/forgot`, `/password/reset` | password/* | `PasswordResetController` → `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | verify-email | `EmailVerificationController` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` | chat, compose, fragments/chat-pane | `ConversationController`, `ChatEventsController` → `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/threads/**`, `/senders` | redirect | `ChatRedirectController` (legacy → /chats) |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | mailboxes/* | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches`, `/searches/{id}/delete` | (redirects) | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | account-* | `AccountController` → `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`; `POST /billing/webhook` | billing/* | `BillingController`, `BillingWebhookController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | team/* | `TeamInviteController` |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/admin/revenue` (+ POST reconcile/win-back) | admin/revenue | `AdminRevenueController` → `*MetricsService` |
| `/privacy`, `/terms`, `/refund` | legal | `LegalController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | — | `SeoController` |
