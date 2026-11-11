# Website (Thymeleaf)

Page inventory reverse-engineered from the controllers and
`src/main/resources/templates` on 2026-11-11. The website and the REST API
([API.md](API.md)) share the service layer — see
[BACKEND.md](BACKEND.md#how-the-web-pages-and-the-rest-api-relate).

## Public pages

| Route | Template | Controller | Services |
|-------|----------|------------|----------|
| `GET /` | `landing` | `web.PricingController` | `LandingProperties`, `BillingProperties` |
| `GET /pricing` | `pricing` | `web.PricingController` | `BillingProperties` |
| `GET /demo` | `conversation` | `web.DemoController` | `DemoConversationService` |
| `GET /privacy`, `/terms`, `/refund` | `legal` | `web.LegalController` | `LegalProperties` |
| `GET /login` | `login` | `auth.AuthController` | — (form posts to Spring Security `POST /login`) |
| `GET/POST /register` | `register` | `auth.AuthController` | `UserService`, `EmailVerificationService` |
| `GET /auth/google/start` | redirect | `auth.OAuthStartController` | `OAuthIntentStore` |
| `GET/POST /password/forgot`, `/password/reset` | `password/forgot`, `password/reset` | `auth.PasswordResetController` | `PasswordResetService` |
| `GET /verify-email` | `verify-email` | `auth.EmailVerificationController` | `EmailVerificationService` |
| `GET /digest/opt-out` | `digest/opt-out` | `digest.DigestController` | digest preferences |
| `GET /billing/cancel` | redirect | `billing.BillingController` | — |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | — | `web.SeoController` | `SiteProperties` |
| `GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png` | — | `web.PwaController` | — |

## Signed-in app

| Route | Template | Controller | Services |
|-------|----------|------------|----------|
| `GET /chats` | `chat` | `web.ConversationController` | `ConversationListService`, `MailboxPollingService`, `MailAccountService`, `BillingBannerService` |
| `GET /chats/{key}` | `chat` | 〃 | + `ChatService` (timeline, mark read) |
| `GET /chats/{key}/pane` | `fragments/chat-pane :: pane` | 〃 | `ChatService` |
| `POST /chats/{key}/send` | redirect | 〃 | `ReplyService`, `OutboundMessageService` |
| `GET/POST /chats/new` | `compose` | 〃 | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/refresh` | JSON `{imported}` | 〃 | `MailboxPollingService.refreshActiveUserNow` |
| `GET /chats/events` | SSE | `web.ChatEventsController` | `ChatEventStream` |
| `GET /threads`, `/threads/**`, `/senders` | redirect to `/chats` | `web.ChatRedirectController` | — |
| `GET /mailboxes`, `GET /mailboxes/new`, `POST /mailboxes`, `POST /mailboxes/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `POST /mailboxes/preferences` | `mailboxes/index`, `mailboxes/new` | `web.MailboxController` | `MailAccountService`, `MailboxPollingService`, `PlanLimitService` |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | redirect | `web.GmailConnectController` | `GmailOAuthClient`, `MailAccountService` |
| `POST /searches`, `POST /searches/{id}/delete` | redirect | `web.SavedSearchController` | `SavedSearchService` |
| `GET /account`, `/account/notifications`, `/account/appearance`, `/account/privacy`; `POST /account/appearance`, `/account/password`, `/account/email` | `account*` | `auth.AccountController` | `AccountService`, `AuthEventService` |
| `POST /verify-email/resend` | redirect | `auth.EmailVerificationController` | `EmailVerificationService` |
| `POST /billing/checkout`, `/billing/portal`; `GET /billing/success`; `GET/POST /billing/cancel-subscription` | `billing/success`, `billing/cancel-subscription` | `billing.BillingController` | `BillingService` |
| `GET/POST /team/invite`, `/team/invite/accept` | `team/invite`, `team/accept` | `team.TeamInviteController` | `TeamInviteService` |
| `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `admin/revenue` | `admin.AdminRevenueController` | admin metrics services, `WinBackOutreachService` |

`POST /billing/webhook` (Stripe, signature-verified, CSRF-exempt) is not a
page.

## Shared fragments

`templates/fragments/`:

| Fragment | Used for |
|----------|----------|
| `app-nav` | Signed-in sidebar/nav (model from `web.NavModelAdvice`). |
| `settings-nav` | Tabs across the `/account*` and `/mailboxes` settings pages. |
| `chat-pane` | The open conversation (timeline, reply box, signature rail); also returned alone by `/chats/{key}/pane`. |
| `live-refresh` | SSE + heartbeat wiring that updates the chat list and open pane in place. |
| `seo` | `<head>` meta / Open Graph tags (model from `web.SiteModelAdvice`). |
| `cookie-banner`, `install-banner`, `sw-register` | Consent banner, PWA install prompt, service-worker registration. |

There is no shared layout template; each page composes the fragments it needs.

## Errors

`web.GlobalExceptionHandler` renders `error.html` with an appropriate status
for web controllers. REST controllers in `com.emailmessenger.api` are handled
first by `api.ApiExceptionHandler`, which returns the JSON envelope instead.

## Web behaviour the API does not have yet

See "Not yet in the API" in [API.md](API.md).
