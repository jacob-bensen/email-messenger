# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `billing/`,
`mailboxes/`, `password/`, `team/`, `digest/`).

| Route | Auth | Controller | Template / notes |
|---|---|---|---|
| `GET /`, `/pricing` | public | `PricingController` | `landing`, `pricing` |
| `GET /demo` | public | `DemoController` | demo conversation (`DemoConversationService`) |
| `/login`, `/register` | public | `AuthController` | `login`, `register` |
| `/auth/google/start`, OIDC callback | public | `OAuthStartController` | Google sign-in |
| `/password/forgot`, `/password/reset` (GET/POST) | public | `PasswordResetController` | `password/*` |
| `/verify-email`, `POST /verify-email/resend` | public / user | `EmailVerificationController` | |
| `/privacy`, `/terms`, `/refund` | public | `LegalController` | `legal` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | `PwaController` | PWA shell |
| `/digest/opt-out` | public | `DigestController` | |
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane` | user | `ConversationController` | `chat` |
| `/chats/new` (GET/POST), `POST /chats/refresh`, `POST /chats/{key}/send` | user | `ConversationController` | `compose`; send → `ReplyService`/`OutboundMessageService` |
| `GET /chats/events` | user | `ChatEventsController` | SSE (new-mail signal) |
| `/threads`, `/threads/**`, `/senders` | user | `ChatRedirectController` | legacy → `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | user | `MailboxController` | `mailboxes/*` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | user | `GmailConnectController` | Gmail OAuth |
| `POST /searches`, `/searches/{id}/delete` | user | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email}` | user | `AccountController` | `account-*` |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription` | user (cancel public) | `BillingController` | Stripe |
| `POST /billing/webhook` | public, Stripe signature | `BillingWebhookController` | only `@RestController` |
| `/team/invite`, `/team/invite/accept` | user | `TeamInviteController` | `team/*` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | admin | `AdminRevenueController` | `admin/*` |

Shared model: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler`
→ `error.html`. Live refresh JS: `fragments/live-refresh.html`.
