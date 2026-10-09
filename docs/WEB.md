# WEB.md — Thymeleaf website

Templates live in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).
Global model data: `NavModelAdvice`, `SiteModelAdvice`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/`, `/pricing` | `PricingController` | `landing`, `pricing` |
| `/demo` | `DemoController` (`DemoConversationService`) | `conversation` |
| `/login`, `/register` | `AuthController` | `login`, `register` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `/verify-email` (+`POST /verify-email/resend`) | `EmailVerificationController` | `verify-email` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` |
| `/digest/opt-out` | `DigestController` | `digest/*` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |
| `/manifest.webmanifest`, `/icons/*`, `/sw.js`, `/offline` | `PwaController` | — |

## Authenticated pages
| Route | Controller | Services used |
|---|---|---|
| `/chats` (GET, `page`,`q`,`mailbox`,`filter`) | `ConversationController` | `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` |
| `/chats/new` GET/POST | same | `OutboundMessageService`, plan limits |
| `/chats/{key}`, `/chats/{key}/pane` | same | `ChatService` (markRead, build `ChatConversation`) |
| `POST /chats/{key}/send` | same | `ReplyService` / `OutboundMessageService` |
| `POST /chats/refresh`, `GET /chats/events` | same / `ChatEventsController` | polling service, `ChatEventStream` |
| `/mailboxes`, `/new`, `POST /`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | `AccountController` | `AccountService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | metrics services |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` |

Controllers resolve the user via `UserService.requireByEmail(principal.getName())` and delegate to
services; business logic lives in services, not templates. Live chat updates: `fragments/live-refresh.html`
consumes the SSE stream.
