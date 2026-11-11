# Website (Thymeleaf)

Server-rendered pages under `src/main/resources/templates`, styled by
`static/css`. All pages are authenticated with the browser session unless
marked *public*. Controllers are thin; the services they call are the same ones
the REST API uses where an API equivalent exists (see `docs/BACKEND.md`).

## Shared layout pieces

| Fragment | Used for |
|----------|----------|
| `fragments/app-nav.html` | Signed-in sidebar/top nav (accounts, saved searches); model from `NavModelAdvice`. |
| `fragments/settings-nav.html` | Tabs across the `/account*` and `/mailboxes` settings pages. |
| `fragments/chat-pane.html` | The open-conversation pane (`pane`, `missing`), also returned standalone for in-place swaps. |
| `fragments/live-refresh.html` | Subscribes to the SSE stream and morphs the rail/pane on new mail. |
| `fragments/seo.html` | `<head>` meta/OG tags for public pages; model from `SiteModelAdvice`. |
| `fragments/cookie-banner.html`, `install-banner.html`, `sw-register.html` | Consent banner, PWA install prompt, service-worker registration. |

## Page inventory

### Public / marketing

| Route | Template | Controller → services |
|-------|----------|-----------------------|
| `GET /` | `landing` | `PricingController` → `LandingProperties`, `BillingProperties` |
| `GET /pricing` | `pricing` | `PricingController` |
| `GET /demo` | `conversation` | `DemoController` → `DemoConversationService` (canned thread through the real IM transform) |
| `GET /privacy`, `/terms`, `/refund` | `legal` | `LegalController` → `LegalProperties` |
| `GET /robots.txt`, `/sitemap.xml` | — | `SeoController` |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | — (generated inline) | `PwaController` |

### Auth

| Route | Template | Controller → services |
|-------|----------|-----------------------|
| `GET /login` (POST handled by Spring Security) | `login` | `AuthController`; `LoginThrottleFilter`, `LoginFailureHandler`, `PlanCheckoutSuccessHandler` |
| `GET/POST /register` | `register` | `AuthController` → `UserService`, `EmailVerificationService` |
| `GET /auth/google/start` | — | `OAuthStartController` → `OAuthIntentStore` |
| `GET/POST /password/forgot`, `/password/reset` | `password/forgot`, `password/reset` | `PasswordResetController` → `PasswordResetService` |
| `GET /verify-email`, `POST /verify-email/resend` | `verify-email` | `EmailVerificationController` → `EmailVerificationService` |

### Chats workspace (the product)

| Route | Template | Controller → services | API equivalent |
|-------|----------|-----------------------|----------------|
| `GET /chats` (`page`, `q`, `mailbox`, `filter`) | `chat` | `ConversationController` → `ConversationListService.list/counts`, `MailboxScope`, `MailboxPollingService.refreshDueForUserAsync`, `BillingBannerService` | `GET /api/v1/conversations` |
| `GET /chats/{key}` | `chat` | → `ChatService.markRead` + `buildFor` | `GET /api/v1/conversations/{key}` + `POST …/read` |
| `GET /chats/{key}/pane` | `fragments/chat-pane :: pane` | same, fragment only | same |
| `POST /chats/{key}/send` | redirect | → `ReplyService`, `OutboundMessageService` (send logic currently in the controller) | none yet |
| `GET/POST /chats/new` | `compose` | → `ReplyService`, `OutboundMessageService`, `ConversationKeyService` | none yet |
| `POST /chats/refresh` | JSON `{imported}` | → `MailboxPollingService.refreshActiveUserNow` | none yet |
| `GET /chats/events` | SSE | `ChatEventsController` → `ChatEventStream` | none yet |
| `GET /threads`, `/threads/**`, `/senders` (legacy) | redirect to `/chats` | `ChatRedirectController` | — |
| `POST /searches`, `POST /searches/{id}/delete` | redirect | `SavedSearchController` → `SavedSearchService` | none yet |

### Settings

| Route | Template | Controller → services |
|-------|----------|-----------------------|
| `GET /account`, `POST /account/password`, `POST /account/email` | `account` | `AccountController` → `AccountService`, `AuthEventService`, `BillingService` |
| `GET /account/notifications` | `account-notifications` | `AccountController` |
| `GET/POST /account/appearance` | `account-appearance` | `AccountController` → `AppearanceSettings` |
| `GET /account/privacy` | `account-privacy` | `AccountController` |
| `GET /mailboxes`, `GET /mailboxes/new`, `POST /mailboxes`, `POST /mailboxes/{id}/sync\|signature\|delete`, `POST /mailboxes/preferences` | `mailboxes/index`, `mailboxes/new` | `MailboxController` → `MailAccountService`, `MailboxPollingService`, `PlanLimitService` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | redirect | `GmailConnectController` → `GmailOAuthClient`, `MailAccountService` |
| `GET/POST /team/invite`, `GET/POST /team/invite/accept` | `team/invite`, `team/accept` | `TeamInviteController` → `TeamInviteService`, `TeamService` |

### Billing, digest, admin

| Route | Template | Controller → services |
|-------|----------|-----------------------|
| `POST /billing/checkout`, `POST /billing/portal` | redirect to Stripe | `BillingController` → `BillingService` |
| `GET /billing/success`, `GET /billing/cancel` | `billing/success`, redirect | `BillingController` |
| `GET/POST /billing/cancel-subscription` | `billing/cancel-subscription` | `BillingController` → `BillingService` |
| `POST /billing/webhook` (public, Stripe-signed, CSRF-exempt) | — | `BillingWebhookController` → `BillingService` |
| `GET /digest/opt-out` (public) | `digest/opt-out` | `DigestController` → `WeeklyDigestService` |
| `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `admin/revenue` | `AdminRevenueController` → metrics services; gated by `AdminAuthorizer` (`ADMIN_EMAILS`) |

Errors render `error.html` via `GlobalExceptionHandler`.

## Web-only behaviour the API does not mirror

- Opening a conversation marks it read implicitly; the API separates the read
  (`GET`) from the write (`POST …/read`).
- The dashboard's per-filter counts (`ConversationListService.counts`) and
  per-account colour tags are rendered server-side and not exposed in the API.
- Plan-limit failures redirect to `/threads` with an upgrade modal
  (`GlobalExceptionHandler.planLimit`); the API has no plan-limited endpoint yet.
