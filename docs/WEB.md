# Thymeleaf website

Templates: `src/main/resources/templates/`. Fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public pages (no auth)
| Route | Controller | Template |
|---|---|---|
| `GET /` | `PricingController` (landing) | `landing` |
| `GET /pricing` | `PricingController` | `pricing` |
| `GET /demo` | `DemoController` (`DemoConversationService`) | demo chat |
| `GET /privacy /terms /refund` | `LegalController` | `legal` |
| `GET/POST /login`, `/register` | `AuthController` | `login`, `register` |
| `GET /auth/google/start` | `OAuthStartController` | redirect |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `/verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` | PWA shell |
| `GET /digest/opt-out` | `DigestController` | — |
| `GET /billing/cancel`, `POST /billing/webhook` | `BillingController`/`BillingWebhookController` | webhook = JSON, signature-verified |

## Authenticated pages
| Route | Controller / service | Template |
|---|---|---|
| `GET /chats` (`q`, `filter`, `page`, `mailbox`) | `ConversationController` → `ConversationListService` | `chat` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `ChatService` | `chat`, `fragments/chat-pane` |
| `GET/POST /chats/new` | `OutboundMessageService` | `compose` |
| `POST /chats/{key}/send` (multipart) | `ReplyService`/`OutboundMessageService` | redirect |
| `POST /chats/refresh` | `MailboxPollingService` | redirect |
| `GET /chats/events` | `ChatEventStream` | SSE |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirect to `/chats` |
| `/mailboxes` (list/new/sync/delete/preferences/signature) | `MailboxController` → `MailAccountService` | `mailboxes/*` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | OAuth redirects |
| `/searches` (create/delete) | `SavedSearchController` | redirect |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` |
| `/billing/{checkout,portal,success,cancel-subscription}` | `BillingController` | `billing/*` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` (allowlist) | `admin/revenue` |

Model attributes shared across pages come from `NavModelAdvice` and `SiteModelAdvice`.
