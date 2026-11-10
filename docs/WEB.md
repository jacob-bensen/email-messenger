# Website (Thymeleaf) inventory

Reverse-engineered from the code on 2026-11-10. Routes and parameters are
specified in [`API.md`](./API.md) §3; this file maps pages to templates,
controllers, and services.

Templates: `src/main/resources/templates`. Styles: one stylesheet,
`static/css/main.css`. Page JavaScript is inline in the templates/fragments
(there is no `static/js`).

## Public pages

| Route | Template | Controller | Services / notes |
|---|---|---|---|
| `/` | `landing` | `PricingController` | `LandingProperties`, `LandingVideo`; signed-in users are redirected into the app |
| `/pricing` | `pricing` | `PricingController` | plan pricing display |
| `/demo` | `conversation` | `DemoController` | `DemoConversationService` (canned data, `demoMode`) |
| `/login` | `login` | `AuthController` | Spring Security form login; Google button when OIDC is configured |
| `/register` | `register` | `AuthController` | `UserService`, `EmailVerificationService`; optional checkout hand-off via `plan`/`billing` |
| `/password/forgot`, `/password/reset` | `password/forgot`, `password/reset` | `PasswordResetController` | `PasswordResetService` |
| `/verify-email` | `verify-email` | `EmailVerificationController` | `EmailVerificationService` |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` | content from `resources/legal`, `LegalProperties` |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` | digest preference by token |
| `/offline`, `/sw.js`, `/manifest.webmanifest`, icons | generated | `PwaController` | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | generated | `SeoController` | |

## Signed-in app

| Route | Template | Controller | Services |
|---|---|---|---|
| `/chats`, `/chats/{key}` | `chat` (+ `fragments/chat-pane`) | `ConversationController` | `ConversationListService` (list + counts), `ChatService` (timeline, mark read), `MailAccountService`, `MailboxPollingService` (async refresh on open), `BillingBannerService` |
| `/chats/{key}/pane` | `fragments/chat-pane :: pane` / `:: missing` | `ConversationController` | `ChatService` |
| `/chats/new` | `compose` | `ConversationController` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService`, `OwnerAddressService` |
| `/chats/{key}/send` (POST) | redirect, or `chat` on validation error | `ConversationController` | `ReplyService`, `OutboundMessageService` |
| `/chats/refresh` (POST), `/chats/events` | JSON / SSE | `ConversationController`, `ChatEventsController` | `MailboxPollingService`, `ChatEventStream` |
| `/mailboxes`, `/mailboxes/new` | `mailboxes/index`, `mailboxes/new` | `MailboxController` | `MailAccountService`, `PlanLimitService`, `MailboxProvider` presets |
| `/mailboxes/gmail/*` | redirects | `GmailConnectController` | `GmailOAuthClient`, `MailAccountService` |
| `/account` | `account` | `AccountController` | `AccountService`, `AccountBillingSummary` |
| `/account/notifications` | `account-notifications` | `AccountController` | static today (no settings persisted) |
| `/account/appearance` | `account-appearance` | `AccountController` | `AppearanceSettings` (theme + accent) |
| `/account/privacy` | `account-privacy` | `AccountController` | static |
| `/billing/success` | `billing/success` | `BillingController` | `BillingService` |
| `/billing/cancel-subscription` | `billing/cancel-subscription` | `BillingController` | `BillingService`, `CancellationReason` |
| `/team/invite`, `/team/invite/accept` | `team/invite`, `team/accept` | `TeamInviteController` | `TeamInviteService`, `TeamService` |
| `/searches` (POST only) | redirect | `SavedSearchController` | `SavedSearchService` |
| `/admin/revenue` | `admin/revenue` | `AdminRevenueController` | the `admin` metrics services, `WinBackOutreachService` |
| `/threads`, `/threads/**`, `/senders` | redirect → `/chats` | `ChatRedirectController` | legacy bounce |
| any error | `error` | `GlobalExceptionHandler` | |

## Fragments

| Fragment | Used by | Purpose |
|---|---|---|
| `fragments/app-nav` | chat, compose, account*, mailboxes/index | signed-in navigation (model from `NavModelAdvice`) |
| `fragments/settings-nav` | account*, mailboxes/index | settings sub-navigation |
| `fragments/chat-pane` | chat, and served standalone at `/chats/{key}/pane` | conversation timeline + reply box + signature rail |
| `fragments/live-refresh` | chat | SSE subscription + 60 s `/chats/refresh` heartbeat; calls `window.ConexusLive.refresh()` |
| `fragments/seo` | public pages | meta/OG tags (model from `SiteModelAdvice`) |
| `fragments/cookie-banner` | public pages | consent banner |
| `fragments/install-banner` | landing | PWA install prompt |
| `fragments/sw-register` | chat, conversation, seo | service-worker registration |

## Shared model advice

- `NavModelAdvice` — navigation state (connected mailboxes, active section).
- `SiteModelAdvice` — site-wide properties (`SiteProperties`).

## Known web issues

Tracked in `INTERNAL_TODO.md` (flash messages lost through the `/threads`
bounce, the never-rendered upgrade modal, the dead reply form in
`conversation.html`).
