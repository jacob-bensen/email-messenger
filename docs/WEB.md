# WEB.md — Thymeleaf website

All routes need an authenticated session unless marked public. Templates: `src/main/resources/templates`, shared fragments in `templates/fragments`.

| Route | Controller | Template | Notes |
|---|---|---|---|
| `GET /`, `/pricing` (public) | `PricingController` | `landing`, `pricing` | |
| `GET /demo` (public) | `DemoController` | `conversation` | `DemoConversationService` |
| `/login`, `/register` (public) | `AuthController` | `login`, `register` | |
| `/password/forgot`, `/password/reset` (public) | `PasswordResetController` | `password/*` | |
| `/verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` | |
| `/auth/google/start` (public) | `OAuthStartController` | — | |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | `AccountController` | `account*` | |
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send` | `ConversationController` | `chat`, `compose`, `fragments/chat-pane` | main workspace |
| `GET /chats/events` | `ChatEventsController` | SSE | |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` | legacy |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes[/{id}/sync|/{id}/delete|/{id}/signature|/preferences]` | `MailboxController` | `mailboxes/*` | |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | — | OAuth |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | — | |
| `POST /billing/{checkout,portal,cancel-subscription}`, `GET /billing/{success,cancel,cancel-subscription}` | `BillingController` | `billing/*` | |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | |
| `/digest/opt-out` (public) | `DigestController` | `digest/*` | |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/*` | operator allowlist |
| `/privacy`, `/terms`, `/refund` (public) | `LegalController` | `legal` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` (public) | `PwaController` | — | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` (public) | `SeoController` | — | |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE morph), `seo`, `cookie-banner`, `install-banner`, `sw-register`.
Known issue: `ConversationController` contains send/recipient/signature logic that is not in a service (see `INTERNAL_TODO.md`).
