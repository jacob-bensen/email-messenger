# Website (Thymeleaf)

Page inventory reverse-engineered from the controllers and templates on
2026-11-11. Templates live in `src/main/resources/templates`; the single
stylesheet is `static/css/main.css`. Page JavaScript is inline in templates
and fragments — there is no `static/js`.

Unless marked public, a page requires a signed-in session.

## Marketing and public pages

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /` (public) | `landing`; signed-in users are redirected to `/threads` → `/chats` | `web.PricingController` | `LandingProperties` |
| `GET /pricing` (public) | `pricing` | `web.PricingController` | — |
| `GET /demo` (public) | `conversation` | `web.DemoController` | `DemoConversationService` |
| `GET /privacy`, `/terms`, `/refund` (public) | `legal` | `web.LegalController` | `LegalProperties`, `ResourceLoader` |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` (public) | generated | `web.SeoController` | `SiteProperties` |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` (public) | generated | `web.PwaController` | — |

## Auth

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /login` (public); `POST /login` is handled by Spring Security | `login` | `auth.AuthController` | — (throttling is `LoginThrottleFilter` in the security chain) |
| `GET`/`POST /register` (public) | `register` | `auth.AuthController` | `UserService`, `BillingService`, `EmailVerificationService` |
| `GET /auth/google/start` (public) | redirect | `auth.OAuthStartController` | `OAuthIntentStore` |
| `GET /verify-email` (public), `POST /verify-email/resend` | `verify-email` | `auth.EmailVerificationController` | `EmailVerificationService` |
| `GET`/`POST /password/forgot`, `/password/reset` (public) | `password/forgot`, `password/reset` | `auth.PasswordResetController` | `PasswordResetService` |
| `POST /logout` | redirect to `/login?logout` | Spring Security | — |

## Chats workspace

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /chats` | `chat` (list, empty pane) | `web.ConversationController` | `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` |
| `GET /chats/{key}` | `chat` (list + open pane); marks read | same | plus `ChatService` |
| `GET /chats/{key}/pane` | fragment `fragments/chat-pane :: pane` or `:: missing` | same | `ChatService` |
| `POST /chats/{key}/send` | redirect to `/chats/{key}` | same | `ReplyService`, `OutboundMessageService` |
| `GET`/`POST /chats/new` | `compose` | same | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/refresh` | JSON, see [API.md](API.md) | same | `MailboxPollingService` |
| `GET /chats/events` | SSE, see [API.md](API.md) | `web.ChatEventsController` | `ChatEventStream` |
| `GET /threads`, `/threads/**`, `/senders` | redirect to `/chats` (legacy URLs) | `web.ChatRedirectController` | — |
| `POST /searches`, `POST /searches/{id}/delete` | redirect | `web.SavedSearchController` | `SavedSearchService` |

`ConversationController` contains business logic that belongs in a service
(sender-account choice, signature appending, recipient parsing, mailbox
scoping, send orchestration). See `INTERNAL_TODO.md`.

## Mailboxes

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /mailboxes` | `mailboxes/index` | `web.MailboxController` | `MailAccountService`, `PlanLimitService`, `PollingPolicy`, `MailAccountRepository` (direct) |
| `GET /mailboxes/new`, `POST /mailboxes` | `mailboxes/new` | same | `MailAccountService` |
| `POST /mailboxes/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | redirect to `/mailboxes` | same | `MailAccountService`, `MailboxPollingService` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | redirects | `web.GmailConnectController` | `GmailOAuthClient`, `MailAccountService` |

## Account, billing, team

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /account`, `POST /account/password`, `/account/email` | `account` | `auth.AccountController` | `AccountService`, `UserService`, `AuthEventService`, `SubscriptionRepository` (direct) |
| `GET /account/notifications` | `account-notifications` | same | — |
| `GET`/`POST /account/appearance` | `account-appearance` | same | `AppearanceSettings` (cookie-backed) |
| `GET /account/privacy` | `account-privacy` | same | — |
| `POST /billing/checkout`, `/billing/portal` | redirect to Stripe | `billing.BillingController` | `BillingService`, `UserService` |
| `GET /billing/success` | `billing/success` | same | `BillingService` |
| `GET /billing/cancel` (public) | redirect to `/pricing` | same | — |
| `GET`/`POST /billing/cancel-subscription` | `billing/cancel-subscription` | same | `BillingService` |
| `GET`/`POST /team/invite` | `team/invite` | `team.TeamInviteController` | `TeamInviteService`, `TeamService`, `TeamInviteRepository` (direct) |
| `GET`/`POST /team/invite/accept` | `team/accept` | same | `TeamInviteService` |
| `GET /digest/opt-out` (public) | `digest/opt-out` | `digest.DigestController` | `DigestEmailPreferenceRepository` (direct, no service) |

## Operator

| Route | Template | Controller | Services |
| --- | --- | --- | --- |
| `GET /admin/revenue` | `admin/revenue` | `admin.AdminRevenueController` | the `admin.*MetricsService` classes |
| `POST /admin/revenue/reconcile-billing-period` | redirect | same | `BillingPeriodBackfillService` |
| `POST /admin/retention/win-back` | redirect | same | `WinBackOutreachService` |

Non-operators get a 404 (`AdminAuthorizer`).

## Shared fragments and model advice

| Fragment | Role |
| --- | --- |
| `fragments/app-nav` | Signed-in navigation with the mailbox list |
| `fragments/settings-nav` | Settings sub-navigation |
| `fragments/chat-pane` | The open conversation: timeline, reply box, signature rail |
| `fragments/live-refresh` | SSE subscription and the `/chats/refresh` heartbeat |
| `fragments/seo` | Meta, Open Graph, canonical tags |
| `fragments/sw-register`, `install-banner` | PWA service worker and install prompt |
| `fragments/cookie-banner` | Cookie consent |

`NavModelAdvice` adds the navigation model (mailboxes, plan, appearance
cookies) to every view; `SiteModelAdvice` adds `siteBaseUrl` and
`oauthGoogleEnabled`. `error.html` is rendered by `GlobalExceptionHandler`.

There is no shared layout template; each page composes the fragments itself.
