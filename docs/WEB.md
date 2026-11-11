# Website (Thymeleaf)

Server-rendered pages under `src/main/resources/templates`, served by the
controllers listed below. Pages authenticate with the session cookie and
every form posts a CSRF token. The JSON API for mobile is separate
(`docs/API.md`); the two share the service layer described in
`docs/BACKEND.md`.

## Page inventory

### Public

| Route | Template | Controller | Services |
|---|---|---|---|
| `GET /` | `landing` | `PricingController` | `LandingProperties`, billing config |
| `GET /pricing` | `pricing` | `PricingController` | billing config |
| `GET /demo` | `conversation` | `DemoController` | `DemoConversationService` (canned data) |
| `GET /login` | `login` | `AuthController` | Spring Security form login; Google button when configured |
| `GET, POST /register` | `register` | `AuthController` | `UserService`, `EmailVerificationService` |
| `GET /auth/google/start` | — (redirect) | `OAuthStartController` | `OAuthIntentStore` |
| `GET, POST /password/forgot`, `/password/reset` | `password/forgot`, `password/reset` | `PasswordResetController` | `PasswordResetService` |
| `GET /verify-email` | `verify-email` | `EmailVerificationController` | `EmailVerificationService` |
| `GET /privacy`, `/terms`, `/refund` | `legal` | `LegalController` | static HTML in `resources/legal/` |
| `GET /digest/opt-out` | `digest/opt-out` | `DigestController` | `WeeklyDigestService` |
| `GET /billing/cancel` | — (redirect) | `BillingController` | — |
| `POST /billing/webhook` | — | `BillingWebhookController` | `BillingService` (Stripe signature verified) |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | — | `SeoController` | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | — | `PwaController` | — |

### Signed-in

| Route | Template | Controller | Services |
|---|---|---|---|
| `GET /chats` | `chat` | `ConversationController` | `ConversationListService`, `MailboxScope`, `MailboxPollingService`, `BillingBannerService` |
| `GET /chats/{key}` | `chat` | `ConversationController` | + `ChatService` (timeline; marks read) |
| `GET /chats/{key}/pane` | `fragments/chat-pane :: pane` | `ConversationController` | `ChatService` — HTML fragment swapped in place |
| `POST /chats/{key}/send` | redirect | `ConversationController` | `ReplyService`, `OutboundMessageService` |
| `GET, POST /chats/new` | `compose` | `ConversationController` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/refresh` | JSON `{"imported": n}` | `ConversationController` | `MailboxPollingService` — page heartbeat, not part of the mobile API |
| `GET /chats/events` | SSE | `ChatEventsController` | `ChatEventStream` |
| `GET /threads`, `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` | legacy URLs |
| `GET, POST /mailboxes`, `/mailboxes/new` | `mailboxes/index`, `mailboxes/new` | `MailboxController` | `MailAccountService`, `PlanLimitService` |
| `POST /mailboxes/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | redirect | `MailboxController` | `MailAccountService`, `MailboxPollingService` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | redirect | `GmailConnectController` | `GmailOAuthClient`, `MailAccountService` |
| `POST /searches`, `/searches/{id}/delete` | redirect | `SavedSearchController` | `SavedSearchService` |
| `GET /account`, `/account/notifications`, `/account/appearance`, `/account/privacy` | `account`, `account-*` | `AccountController` | `AccountService`, `AuthEventService`, billing summary |
| `POST /account/password`, `/account/email`, `/account/appearance` | redirect | `AccountController` | `AccountService` |
| `POST /verify-email/resend` | redirect | `EmailVerificationController` | `EmailVerificationService` |
| `POST /billing/checkout`, `/billing/portal`; `GET /billing/success` | redirect / `billing/success` | `BillingController` | `BillingService` |
| `GET, POST /billing/cancel-subscription` | `billing/cancel-subscription` | `BillingController` | `BillingService` |
| `GET, POST /team/invite`, `/team/invite/accept` | `team/invite`, `team/accept` | `TeamInviteController` | `TeamInviteService`, `TeamService` |
| `GET /admin/revenue`; `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `admin/revenue` | `AdminRevenueController` | metrics services; gated by `AdminAuthorizer` |

## Shared fragments (`templates/fragments/`)

| Fragment | Used for |
|---|---|
| `app-nav` | Signed-in sidebar/nav: mailbox list, saved searches (model from `NavModelAdvice`) |
| `settings-nav` | Tabs across the `/account*` and `/mailboxes` settings pages |
| `chat-pane` | The open conversation: timeline, reply box, signature rail. Rendered inside `chat` and standalone by `/chats/{key}/pane` |
| `live-refresh` | Subscribes to `/chats/events` and morphs the list + open pane when mail arrives |
| `seo` | `<head>` meta, canonical, Open Graph (model from `SiteModelAdvice`) |
| `cookie-banner`, `install-banner`, `sw-register` | Consent banner, PWA install prompt, service-worker registration |

There is no shared layout template; each page composes the fragments it needs.

## Errors

`web.GlobalExceptionHandler` renders `error.html` with an appropriate status
for every controller outside `com.emailmessenger.api`. A
`PlanLimitExceededException` instead redirects with an upgrade modal.

## Website ↔ API parity

| Capability | Website | API |
|---|---|---|
| Sign in with password | `/login` | `POST /api/v1/auth/login` |
| Sign out | `/logout` | `POST /api/v1/auth/logout` |
| Chats list, search, filters, mailbox scope | `GET /chats` | `GET /api/v1/conversations` |
| Everything else in the tables above | yes | **no** |
