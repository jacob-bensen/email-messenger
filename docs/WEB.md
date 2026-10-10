# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`. Controllers are package-private classes in `web/`, `auth/`, `billing/`, `team/`, `digest/`, `admin/`.

## Public pages
`/` landing, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/login`, `/register`, `/password/forgot`, `/password/reset`, `/verify-email`, `/digest/opt-out`, `/billing/cancel`, `/offline`, plus `robots.txt`, `sitemap.xml`, `manifest.webmanifest`, `sw.js`, icons (`SeoController`, `PwaController`, `LegalController`, `PricingController`, `DemoController`, `AuthController`, `PasswordResetController`, `EmailVerificationController`, `DigestController`).

## Authenticated pages
| Route | Controller → services | Template |
|---|---|---|
| `GET /chats` (`q`, `mailbox`, `filter`, `page`) | `ConversationController` → `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` | `chat.html` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `ChatService`, `ConversationKeyService` | `chat.html`, `fragments/chat-pane.html` |
| `POST /chats/{key}/send` | `ReplyService`/`OutboundMessageService` (SMTP, attachments multipart) | redirect |
| `GET/POST /chats/new` | `OutboundMessageService` | `compose.html` |
| `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | `MailboxPollingService`, `ChatEventStream` | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/delete`, `/{id}/sync`, `/{id}/signature`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` → `MailAccountService` | `mailboxes/*` |
| `/searches` | `SavedSearchController` → `SavedSearchService` | redirects into `/chats` |
| `/account`, `/account/{appearance,notifications,privacy,email,password}` | `AccountController` → `AccountService` | `account*.html` |
| `/billing/{checkout,portal,success,cancel-subscription}`, `POST /billing/webhook` | `BillingController`, `BillingWebhookController` → `BillingService` | `billing/*` |
| `/team/invite[/accept]` | `TeamInviteController` | `team/*` |
| `/admin/revenue`, admin POSTs | `AdminRevenueController` (`AdminAuthorizer`) | `admin/revenue.html` |
| `/threads`, `/senders` | `ChatRedirectController` → redirect to `/chats` | — |

Shared fragments: `app-nav`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`, `live-refresh` (SSE-driven in-place rail/pane morph), `sw-register`. Global model attributes via `NavModelAdvice`, `SiteModelAdvice`; errors via `GlobalExceptionHandler`/`error.html`.
