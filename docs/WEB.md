# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates/`. Shared fragments in
`templates/fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`,
`seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model
attributes come from `NavModelAdvice` / `SiteModelAdvice`; errors from
`GlobalExceptionHandler` → `error.html`.

## Page inventory (route → controller → template)
| Route | Controller | Template | Auth |
|---|---|---|---|
| `GET /`, `/pricing` | `PricingController` | `landing`, `pricing` | public |
| `GET /demo` | `DemoController` | `chat` (demo data) | public |
| `/login`, `/register` (GET/POST), `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` | public |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | public |
| `/verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` | public / user |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` | public |
| `GET /chats` (`page,q,mailbox,filter`) | `ConversationController` | `chat` | user |
| `GET /chats/new`, `POST /chats/new`, `POST /chats/refresh` | `ConversationController` | `compose` | user |
| `GET /chats/{key}`, `GET /chats/{key}/pane` | `ConversationController` | `chat`, `fragments/chat-pane` | user |
| `POST /chats/{key}/send` (multipart) | `ConversationController` | `chat` | user |
| `GET /chats/events` (SSE) | `ChatEventsController` | — | user |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect → `/chats` | user |
| `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | `MailboxController` | `mailboxes/*` | user |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | redirect | user |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | redirect | user |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{appearance,password,email}` | `AccountController` | `account*` | user |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `billing/*` | mixed |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | user |
| `/digest/opt-out` | `DigestController` | `digest/*` | public |
| `/admin/revenue` + POST actions | `AdminRevenueController` | `admin/*` | admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` | — | public |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — | public |

Service usage: chat pages → `ConversationListService`, `ChatService`,
`ReplyService`, `OutboundMessageService`, `MailboxPollingService`,
`MailAccountService`; billing banners → `BillingBannerService`.
