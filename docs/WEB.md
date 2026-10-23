# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(e.g. `live-refresh.html`). Static assets under `static/` (`/css`, `/js`, `/images`).
Public = permitAll in `SecurityConfig`; everything else needs login.

| Route | Controller | Auth | Notes |
|---|---|---|---|
| `GET /`, `/pricing` | `PricingController` | public | landing / pricing |
| `GET /demo` | `DemoController` | public | `DemoConversationService` |
| `GET /privacy /terms /refund` | `LegalController` | public | |
| `GET /login`, `GET/POST /register`, `GET /auth/google/start` | `AuthController`, `OAuthStartController` | public | form + Google OIDC |
| `/password/forgot`, `/password/reset` (GET/POST) | `PasswordResetController` | public | |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | public / user | |
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send`; `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController` | user | main workspace; services: `ChatService`, `ConversationListService`, `ReplyService` |
| `GET /threads`, `/threads/**`, `/senders` | `ChatRedirectController` | user | legacy redirects to `/chats` |
| `/mailboxes` (list/new/create), `POST /mailboxes/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | user | `MailAccountService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | user | Gmail OAuth |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | user | |
| `/account`, `/account/notifications`, `/account/appearance` (GET/POST), `/account/privacy`, `POST /account/password`, `POST /account/email` | `AccountController` | user | |
| `POST /billing/checkout`, `/billing/portal`; `GET /billing/success`, `/billing/cancel`; `GET/POST /billing/cancel-subscription` | `BillingController` | user (`/billing/cancel` public) | Stripe |
| `/team/invite` (GET/POST), `/team/invite/accept` (GET/POST) | `TeamInviteController` | user | |
| `GET /digest/opt-out` | `DigestController` | public | |
| `GET /admin/revenue`; `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | operator allowlist (404 otherwise) | |
| `/manifest.webmanifest`, `/icons/*`, `/apple-touch-icon.png`, `/sw.js`, `/offline` | `PwaController` | public | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | public | |

Model advice shared by all pages: `NavModelAdvice`, `SiteModelAdvice`.
Errors: `GlobalExceptionHandler` (e.g. 413 on oversized uploads).
