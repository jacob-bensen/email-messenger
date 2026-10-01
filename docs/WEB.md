# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`,
`chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model
attributes come from `NavModelAdvice` and `SiteModelAdvice`; errors via `GlobalExceptionHandler` + `error.html`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `GET /` , `/pricing` | `PricingController` | `landing.html`, `pricing.html` |
| `GET /demo` | `DemoController` (`DemoConversationService`) | `conversation.html` |
| `GET /privacy /terms /refund` | `LegalController` | `legal.html` |
| `GET /login /register`, `POST /register` (login POST handled by Spring Security) | `AuthController` | `login.html`, `register.html` |
| `GET /auth/google/start` | `OAuthStartController` | redirect |
| `GET/POST /password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `GET /verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email.html` |
| `GET /digest/opt-out` | `DigestController` | `digest/*` |
| `/robots.txt /sitemap.xml /images/og-card.png` | `SeoController` | generated |
| `/manifest.webmanifest /sw.js /offline /icons/*` | `PwaController` | generated |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter`), `GET /chats/{key}`, `GET /chats/{key}/pane` | `ConversationController` | `ConversationListService`, `ChatService`, `MailboxPollingService` |
| `GET/POST /chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `ReplyService`, `OutboundMessageService`, `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects to `/chats` |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService` |
| `/billing/checkout /portal /success /cancel-subscription` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/admin/revenue` (+ win-back / reconcile POSTs) | `AdminRevenueController` | `admin/*MetricsService`; gated by `AdminAuthorizer` |

Business rules live in services; controllers only parse params, call services, and populate the model.
