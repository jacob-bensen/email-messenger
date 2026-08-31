# WEB.md — Thymeleaf website (email-messenger)

The Spring Boot app in this repo BOTH serves the public website
(server-rendered Thymeleaf pages) AND — planned — a JSON REST API
for the Flutter mobile client at `conexus-mail-mobile` (see
`docs/API.md`). The rule is simple and non-negotiable: web pages and
API endpoints must share the same service layer. Never duplicate
business logic between a controller and a template.

## Page inventory

Grouped by area. Each row lists the route(s), the controller class,
and the service(s) each page reads from — so a template refactor can
tell at a glance whether it's touching business logic or presentation.

### Marketing / SEO

| Route | Controller | Services |
| --- | --- | --- |
| `GET /` | `PricingController#landing` | `PlanPricing` |
| `GET /pricing` | `PricingController#pricing` | `PlanPricing`, `BillingService` |
| `GET /demo` | `DemoController#demo` | — (static fixture data) |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | — |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |

### Auth (form + Google OIDC)

| Route | Controller | Services |
| --- | --- | --- |
| `GET/POST /login` | `AuthController` | Spring Security |
| `GET/POST /register` | `AuthController` | `UserService`, `AccountService`, `EmailVerificationService` |
| `GET /auth/google/start` | `OAuthStartController` | `OAuthIntentStore` |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `EmailVerificationService` |
| `GET/POST /password/forgot`, `GET/POST /password/reset` | `PasswordResetController` | `PasswordResetService` |

### Account settings

| Route | Controller | Services |
| --- | --- | --- |
| `GET /account` | `AccountController#account` | `AccountService`, `AccountBillingSummary` |
| `GET /account/notifications` | `AccountController#notifications` | — (static page pending Web Push work) |
| `GET/POST /account/appearance` | `AccountController` | `AppearanceSettings` |
| `GET /account/privacy` | `AccountController#privacy` | — |
| `POST /account/password` | `AccountController#changePassword` | `UserService` |
| `POST /account/email` | `AccountController#changeEmail` | `UserService`, `EmailVerificationService` |

### Chats (the core /chats workspace)

| Route | Controller | Services |
| --- | --- | --- |
| `GET /chats` | `ConversationController#list` | `ChatService`, `MailboxPollingService`, `MailAccountService` |
| `GET /chats/new`, `POST /chats/new` | `ConversationController` | `ChatService`, `ReplyService` |
| `GET /chats/{key}` | `ConversationController#chat` | `ChatService`, `MailboxPollingService` |
| `GET /chats/{key}/pane` | `ConversationController#chatPane` | `ChatService` |
| `POST /chats/{key}/send` | `ConversationController#send` | `ReplyService` |
| `POST /chats/refresh` **(JSON)** | `ConversationController#refresh` | `MailboxPollingService` |
| `GET /chats/events` **(SSE)** | `ChatEventsController#events` | `ChatEventStream` |
| `GET /threads/**`, `/senders` → 301 `/chats` | `ChatRedirectController` | — |

### Saved searches

| Route | Controller | Services |
| --- | --- | --- |
| `GET /searches` (embedded in `/chats`) | via `ConversationController` | `SavedSearchService` |
| `POST /searches` | `SavedSearchController#save` | `SavedSearchService`, `PlanLimitEnforcer` |
| `POST /searches/{id}/delete` | `SavedSearchController#delete` | `SavedSearchService` |

### Mailboxes

| Route | Controller | Services |
| --- | --- | --- |
| `GET /mailboxes` | `MailboxController#listMailboxes` | `MailAccountService` |
| `GET /mailboxes/new`, `POST /mailboxes` | `MailboxController` | `MailAccountService`, `PlanLimitEnforcer` |
| `POST /mailboxes/{id}/sync` | `MailboxController#syncMailbox` | `MailboxPollingService` |
| `POST /mailboxes/preferences` | `MailboxController#updatePreferences` | `UserMailPreferences` service |
| `POST /mailboxes/{id}/signature` | `MailboxController#saveSignature` | `MailAccountService` |
| `POST /mailboxes/{id}/delete` | `MailboxController#deleteMailbox` | `MailAccountService` |
| `GET /mailboxes/gmail/connect`, `/callback`, `/reconnect/{id}` | `GmailConnectController` | `GmailOAuthService` |

### Billing

| Route | Controller | Services |
| --- | --- | --- |
| `POST /billing/checkout` | `BillingController#startCheckout` | `BillingService` |
| `POST /billing/portal` | `BillingController#openPortal` | `BillingService` |
| `GET /billing/success`, `/cancel` | `BillingController` | — |
| `GET/POST /billing/cancel-subscription` | `BillingController` | `BillingService` |
| `POST /billing/webhook` **(JSON in)** | `BillingWebhookController` | `BillingService`, Stripe SDK |

### Teams

| Route | Controller | Services |
| --- | --- | --- |
| `GET/POST /team/invite` | `TeamInviteController` | `TeamService`, `TeamInviteService` |
| `GET/POST /team/invite/accept` | `TeamInviteController` | `TeamService`, `TeamInviteService` |

### Digests

| Route | Controller | Services |
| --- | --- | --- |
| `GET /digest/opt-out` | `DigestController#optOut` | `DigestEmailPreferenceService` |

### Admin (`ADMIN_EMAILS`-gated)

| Route | Controller | Services |
| --- | --- | --- |
| `GET /admin/revenue` | `AdminRevenueController#revenue` | `RevenueMetricsService`, `ChurnMetricsService`, `ProAdoptionMetricsService`, `OnboardingFunnelMetricsService`, `TrialEndConversionMetricsService`, `AtRiskRetentionService`, `WinBackConversionMetricsService`, `AdminWeeklyDigestService` |
| `POST /admin/retention/win-back` | `AdminRevenueController#sendWinBack` | `WinBackOutreachService` |
| `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController#reconcileBillingPeriod` | `BillingPeriodBackfillService` |

### PWA shell

| Route | Controller | Services |
| --- | --- | --- |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, icon PNGs | `PwaController` | — |

## Shared fragments

- `fragments/app-nav.html` — top nav on authenticated pages
- `fragments/settings-nav.html` — sub-nav on `/account/*` pages
- `fragments/chat-pane.html` — the open conversation column,
  re-rendered by `GET /chats/{key}/pane`
- `fragments/live-refresh.html` — client JS that consumes
  `/chats/events` and morphs the list/pane in place
- `fragments/cookie-banner.html`, `install-banner.html`,
  `sw-register.html` — cross-page shells
- `fragments/seo.html` — meta/OG tags per page

## What is JSON today

Only two endpoints currently emit JSON:

- `POST /chats/refresh` → `{"imported": N}` — used by the /chats page
  itself to poll before reloading.
- `POST /billing/webhook` — accepts Stripe's JSON event envelope.

There is one SSE stream: `GET /chats/events` (`text/event-stream`).

Everything else is server-rendered HTML. The REST API surface planned
for the mobile client is documented in `docs/API.md` as "Planned" until
implemented.

## Rules for adding a page

1. Route in an existing controller when the area matches; otherwise a
   new package-private controller class.
2. Read state through a service — never call a repository from a
   controller. If a matching service call doesn't exist, add it to the
   service, not to the controller.
3. Add or extend a test in the mirroring package under
   `src/test/java/...`.
4. If the same behavior needs to reach the mobile client, add or
   update a JSON endpoint in `docs/API.md` first, then implement it in
   the same run.
