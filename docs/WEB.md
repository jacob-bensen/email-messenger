# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (app-nav,
settings-nav, chat-pane, live-refresh, seo, cookie-banner, install-banner,
sw-register). Auth column: P = public, A = authenticated.

| Route | Auth | Controller → main services | Template |
|---|---|---|---|
| `GET /`, `/pricing` | P | `PricingController` | landing, pricing |
| `GET /demo` | P | `DemoController` → `DemoConversationService` | chat |
| `/login`, `/register`, `/auth/google/start` | P | `AuthController`, `OAuthStartController` → `UserService` | login, register |
| `/password/forgot`, `/password/reset` (GET/POST) | P | `PasswordResetController` | password/* |
| `/verify-email`, `POST /verify-email/resend` | P / A | `EmailVerificationController` | verify-email |
| `/privacy`, `/terms`, `/refund` | P | `LegalController` | legal |
| `/digest/opt-out` | P | `DigestController` | digest/opt-out |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | P | `SeoController` | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | P | `PwaController` | — |
| `GET /chats` | A | `ConversationController` → `ConversationListService`, `MailboxPollingService` | chat |
| `/chats/new` (GET/POST) | A | `ConversationController` → `OutboundMessageService` | compose |
| `POST /chats/refresh`, `GET /chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send` | A | `ConversationController` → `ChatService`, `ReplyService` | chat, chat-pane |
| `GET /chats/events` | A | `ChatEventsController` (SSE) | — |
| `/threads`, `/threads/**`, `/senders` | A | `ChatRedirectController` (legacy → /chats) | — |
| `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | A | `MailboxController` → `MailAccountService` | mailboxes/* |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | A | `GmailConnectController` → `GmailOAuthClient` | — |
| `POST /searches`, `POST /searches/{id}/delete` | A | `SavedSearchController` → `SavedSearchService` | — |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/password`, `POST /account/email` | A | `AccountController` → `AccountService` | account-* |
| `POST /billing/checkout`, `/billing/portal`, `GET /billing/success`, `/billing/cancel`, `/billing/cancel-subscription` (GET/POST) | A (cancel P) | `BillingController` → `BillingService` | billing/* |
| `/team/invite` (GET/POST), `/team/invite/accept` (GET/POST) | A | `TeamInviteController` → `TeamInviteService` | team/* |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | A + admin | `AdminRevenueController` → admin metrics services | admin/revenue |
