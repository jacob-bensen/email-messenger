# ConexusMail website — page inventory

Server-rendered Thymeleaf pages served by the backend. Templates live in
`src/main/resources/templates`. Every page goes through the same service
layer the API will use (`docs/BACKEND.md`).

Last audited against the code: 2026-11-16.

## Public pages

| Route | Controller | Template | Services |
|---|---|---|---|
| `GET /` | `PricingController` | `landing` (signed-in users → `/threads` → `/chats`) | `LandingProperties` |
| `GET /pricing` | `PricingController` | `pricing` | — |
| `GET /demo` | `DemoController` | `conversation` | `DemoConversationService` |
| `GET /login`, `GET/POST /register` | `AuthController` | `login`, `register` | `UserService`, `BillingService`, `EmailVerificationService` |
| `GET /auth/google/start` | `OAuthStartController` | redirect to Google | `OAuthIntentStore` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` | `LegalProperties` |
| `GET /digest/opt-out` | `DigestController` | `digest/opt-out` | `DigestEmailPreferenceRepository` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — | `SiteProperties` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | `conexusmail-shell` | — |

## Signed-in pages

| Route | Controller | Template | Services |
|---|---|---|---|
| `GET /chats` | `ConversationController` | `chat` | `ConversationListService`, `ChatService`, `MailAccountService`, `BillingBannerService`, `MailboxPollingService` |
| `GET /chats/{key}` | `ConversationController` | `chat` (list + open pane; marks read) | as above |
| `GET /chats/{key}/pane` | `ConversationController` | `fragments/chat-pane :: pane` | `ChatService` |
| `POST /chats/{key}/send` | `ConversationController` | redirect | `ReplyService`, `OutboundMessageService` (+ inline send logic) |
| `GET/POST /chats/new` | `ConversationController` | `compose` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/refresh`, `GET /chats/events` | `ConversationController`, `ChatEventsController` | JSON / SSE (`API.md` §3) | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirect → `/chats` | — |
| `/mailboxes` (list, `new`, create, `{id}/sync`, `{id}/signature`, `{id}/delete`, `preferences`) | `MailboxController` | `mailboxes/index`, `mailboxes/new` | `MailAccountService`, `PlanLimitService`, `MailboxPollingService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | redirects | `GmailOAuthClient`, `MailAccountService` |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | redirect | `SavedSearchService` |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/password`, `POST /account/email` | `AccountController` | `account*` | `AccountService`, `UserService`, `AuthEventService` |
| `/billing/checkout`, `/billing/portal`, `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription` | `BillingController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService`, `TeamService` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | admin metric services, `WinBackOutreachService` |

## Shared fragments (`templates/fragments`)

`app-nav` (top nav; model from `NavModelAdvice`), `settings-nav`,
`chat-pane` (open conversation), `live-refresh` (SSE + heartbeat wiring),
`seo`, `cookie-banner`, `install-banner`, `sw-register`. Site-wide model
attributes come from `SiteModelAdvice` and `NavModelAdvice`.

## Errors

`GlobalExceptionHandler`: 404 for missing resources / `NoSuchElementException`,
502 for mail failures, rendered with `error`.
