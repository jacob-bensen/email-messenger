# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model: `NavModelAdvice`, `SiteModelAdvice`.

## Public
| Route | Template / handler | Notes |
|---|---|---|
| `GET /` | `landing` | `LandingProperties` |
| `GET /pricing` | `pricing` | `PricingController` |
| `GET /demo` | `chat` (demo data) | `DemoController`, `DemoConversationService` |
| `GET /privacy` `/terms` `/refund` | `legal` | `LegalController` |
| `GET/POST /login`, `/register` | `login`, `register` | `AuthController`; `GET /auth/google/start` |
| `GET/POST /password/forgot`, `/password/reset` | `password/*` | `PasswordResetService` |
| `GET /verify-email`, `POST /verify-email/resend` | `verify-email` | |
| `GET /digest/opt-out` | `digest/opt-out` | |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA/SEO | `SeoController`, `PwaController` |

## Authenticated
| Route | Template | Services |
|---|---|---|
| `GET /chats?page&q&mailbox&filter` | `chat` | `ConversationListService`, `MailAccountService`, `MailboxPollingService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `chat`, `fragments/chat-pane` | `ChatService.markRead/buildFor` |
| `GET/POST /chats/new` | `compose` | `ReplyService.sendNewEmail`, `OutboundMessageService` |
| `POST /chats/{key}/send` | redirect | `ReplyService`, `OutboundMessageService` (+ plan limits) |
| `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | — | `MailboxPollingService`, `ChatEventStream` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `mailboxes/*` | `MailAccountService` |
| `/searches` (POST, `/{id}/delete`) | redirect | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | `account*` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `POST /billing/webhook` (public, Stripe-signed) | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `team/*` | `TeamInviteService` |
| `/admin/revenue`, `POST /admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `admin/revenue` | operator-only (`AdminAuthorizer`) |
| `/threads`, `/threads/**`, `/senders` | redirect to `/chats` | legacy |
