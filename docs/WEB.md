# ConexusMail website — page inventory

Server-rendered Thymeleaf (`src/main/resources/templates`), one stylesheet
(`static/css/main.css`), PWA shell. All pages go through the session/form
security chain (`auth.SecurityConfig`). Shared rules live in services also
used by the API (`docs/API.md` §6).

## Public pages
| Route | Template | Controller → services |
|---|---|---|
| `GET /` | `landing` | `PricingController` → `LandingProperties` |
| `GET /pricing` | `pricing` | `PricingController` |
| `GET /demo` | `conversation` (demo data) | `DemoController` → `DemoConversationService` |
| `GET/POST /login`, `GET/POST /register` | `login`, `register` | `AuthController` → `UserService`; POST /login is Spring form login |
| `GET /auth/google/start` | — | `OAuthStartController` (Google OIDC) |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` → `PasswordResetService` |
| `/verify-email`, `POST /verify-email/resend` | `verify-email` | `EmailVerificationController` |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` |
| `/billing/cancel` | — | `BillingController` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | — | `SeoController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `PwaController` |

## Signed-in pages
| Route | Template | Controller → services |
|---|---|---|
| `GET /chats` | `chat` | `ConversationController` → `ConversationListService`, `ChatService`, `MailAccountService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `chat`, `fragments/chat-pane` | `ConversationController` → `ChatService` |
| `POST /chats/{key}/send` | redirect | `ConversationController` → `ReplyService`, `OutboundMessageService` |
| `GET/POST /chats/new` | `compose` | `ConversationController` |
| `POST /chats/refresh` | JSON count | `ConversationController` → `MailboxPollingService` |
| `GET /chats/events` | SSE | `ChatEventsController` → `ChatEventStream` |
| `/threads/**`, `/senders` | redirect → `/chats` | `ChatRedirectController` |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | `mailboxes/*` | `MailboxController` → `MailAccountService`, `MailboxPollingService`, `PlanLimitService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | — | `GmailConnectController` |
| `POST /searches`, `/searches/{id}/delete` | redirect | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/password`, `POST /account/email` | `account*` | `AccountController` → `AccountService` |
| `/billing/checkout`, `/billing/portal`, `/billing/success`, `/billing/cancel-subscription` | `billing/*` | `BillingController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | `team/*` | `TeamInviteController` |
| `/admin/revenue` (+ POST actions) | `admin/revenue` | `AdminRevenueController` (operator allowlist) |

## Shared fragments
`fragments/app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE
morph), `seo`, `sw-register`, `install-banner`, `cookie-banner`.
`NavModelAdvice` / `SiteModelAdvice` add nav and site model attributes;
`GlobalExceptionHandler` renders `error`.
