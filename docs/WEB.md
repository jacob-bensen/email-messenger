# Website (Thymeleaf)

Page inventory reverse-engineered from the controllers on 2026-11-11.
Templates live in `src/main/resources/templates`; the single stylesheet is
`static/css/main.css`. Scripts are inline in templates/fragments.

## Public pages

| Route | Controller | Template | Services |
|---|---|---|---|
| `GET /` | `PricingController` | `landing` | `LandingProperties`, billing config |
| `GET /pricing` | `PricingController` | `pricing` | billing config |
| `GET /demo` | `DemoController` | `conversation` | `DemoConversationService` |
| `GET /login` | `AuthController` | `login` | — (form posts to Spring Security `POST /login`) |
| `GET,POST /register` | `AuthController` | `register` | `UserService`, `EmailVerificationService` |
| `GET /auth/google/start` | `OAuthStartController` | redirect | `OAuthIntentStore` |
| `GET,POST /password/forgot`, `/password/reset` | `PasswordResetController` | `password/forgot`, `password/reset` | `PasswordResetService` |
| `GET /verify-email` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | `legal` | `LegalProperties` (sources in `resources/legal/`) |
| `GET /digest/opt-out` | `DigestController` | `digest/opt-out` | digest preferences |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | generated | `SiteProperties` |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | `PwaController` | generated | — |

## Authenticated app

| Route | Controller | Template | Services |
|---|---|---|---|
| `GET /chats` (`page`, `q`, `mailbox`, `filter`) | `ConversationController` | `chat` | `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` |
| `GET /chats/{key}` | `ConversationController` | `chat` | + `ChatService` (marks read) |
| `GET /chats/{key}/pane` | `ConversationController` | `fragments/chat-pane :: pane` | `ChatService` |
| `POST /chats/{key}/send` (multipart) | `ConversationController` | redirect | `ReplyService`, `OutboundMessageService` |
| `GET,POST /chats/new` | `ConversationController` | `compose` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/refresh`, `GET /chats/events` | `ConversationController`, `ChatEventsController` | JSON / SSE | see `API.md` |
| `GET /threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect → `/chats` | legacy URLs |
| `GET /mailboxes`, `GET /mailboxes/new`, `POST /mailboxes`, `POST /mailboxes/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `POST /mailboxes/preferences` | `MailboxController` | `mailboxes/index`, `mailboxes/new` | `MailAccountService`, `MailboxPollingService`, `PlanLimitService` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | redirects | `GmailOAuthClient`, `MailAccountService` |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | redirect | `SavedSearchService` |
| `GET /account`, `/account/notifications`, `/account/appearance`, `/account/privacy`; `POST /account/appearance`, `/account/password`, `/account/email` | `AccountController` | `account*` | `AccountService`, `AccountBillingSummary` |
| `POST /verify-email/resend` | `EmailVerificationController` | redirect | `EmailVerificationService` |
| `POST /billing/checkout`, `/billing/portal`; `GET /billing/success`, `/billing/cancel`; `GET,POST /billing/cancel-subscription` | `BillingController` | `billing/*` | `BillingService` |
| `GET,POST /team/invite`, `GET,POST /team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService`, `TeamService` |
| `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | admin metrics services (operator allowlist) |

## Shared fragments

| Fragment | Used for |
|---|---|
| `fragments/app-nav :: nav` | App navigation (model from `NavModelAdvice`). |
| `fragments/settings-nav :: nav(tab)` | Settings tabs: account, accounts, appearance, notifications, privacy. |
| `fragments/chat-pane :: pane` / `:: missing` | Open-conversation pane; also served standalone by `/chats/{key}/pane`. |
| `fragments/live-refresh :: script` | `/chats/refresh` heartbeat + `/chats/events` EventSource, in-place rail/pane update. |
| `fragments/cookie-banner`, `install-banner`, `sw-register`, `seo` | Cookie notice, PWA install prompt, service-worker registration, meta tags (`SiteModelAdvice`). |

There is no layout dialect; each page includes fragments directly.

## Errors

`GlobalExceptionHandler` renders `error.html` for all error statuses, and
turns `PlanLimitExceededException` into a flash `upgradeModal` + redirect.
