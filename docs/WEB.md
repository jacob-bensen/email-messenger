# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates/` (+ `fragments/`, `admin/`, `billing/`, `digest/`,
`mailboxes/`, `password/`, `team/`). Auth column: P = public, U = login required, A = admin.

| Route | Auth | Controller | Template / result |
|---|---|---|---|
| `/`, `/pricing` | P | `PricingController` | `landing`, `pricing` |
| `/demo` | P | `DemoController` | demo chat (`DemoConversationService`) |
| `/privacy` `/terms` `/refund` | P | `LegalController` | `legal` |
| `/login` `/register` | P | `AuthController` | `login`, `register` |
| `/auth/google/start` | P | `OAuthStartController` | OIDC redirect |
| `/password/forgot` `/password/reset` | P | `PasswordResetController` | `password/*` |
| `/verify-email`, POST `/verify-email/resend` | P/U | `EmailVerificationController` | `verify-email` |
| `/chats` (q, mailbox, filter, page) | U | `ConversationController` | `chat` (rail + pane) |
| `/chats/new` GET/POST | U | `ConversationController` | `compose` |
| POST `/chats/refresh` | U | `ConversationController` | poll now |
| `/chats/{key}`, `/chats/{key}/pane` | U | `ConversationController` | `chat` / pane fragment |
| POST `/chats/{key}/send` | U | `ConversationController` | send/reply (multipart) |
| `/chats/events` | U | `ChatEventsController` | SSE |
| `/threads`, `/threads/**`, `/senders` | U | `ChatRedirectController` | legacy redirect |
| `/mailboxes` (+`/new`, POST create, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | U | `MailboxController` | `mailboxes/*` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | U | `GmailConnectController` | Gmail OAuth |
| POST `/searches`, `/searches/{id}/delete` | U | `SavedSearchController` | redirect |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, POST `/account/password`, `/account/email` | U | `AccountController` | `account-*` |
| POST `/billing/checkout`, `/billing/portal`, `/billing/cancel-subscription`; GET `/billing/success`, `/billing/cancel` | U (cancel: P) | `BillingController` | Stripe redirects |
| POST `/billing/webhook` | P (signature) | `BillingWebhookController` | Stripe events |
| `/team/invite`, `/team/invite/accept` | U | `TeamInviteController` | `team/*` |
| `/digest/opt-out` | P | `DigestController` | `digest/*` |
| `/admin/revenue`, POST `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | A | `AdminRevenueController` | `admin/*` |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | P | `SeoController` | generated |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | P | `PwaController` | PWA shell |

Live updates: `fragments/live-refresh.html` consumes `/chats/events` and morphs the rail and
open pane in `chat.html`.
