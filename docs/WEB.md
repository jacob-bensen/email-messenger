# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Controllers are package-private classes.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `GET /` , `/pricing` | `PricingController` | `landing`, `pricing` |
| `GET /demo` | `DemoController` (`DemoConversationService`) | `chat` demo |
| `GET /login`, `/register`, `POST /register` | `AuthController` | `login`, `register` |
| `/password/forgot`, `/password/reset` (GET+POST) | `PasswordResetController` | `password/*` |
| `/verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` |
| `/digest/opt-out` | `DigestController` | `digest/*` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | — |
| PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | — |
| `/auth/google/start` | `OAuthStartController` | redirect |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (list/Hub, `q`,`mailbox`,`filter`=ALL/UNREAD/AWAITING/ATTACHMENTS,`page`) | `ConversationController` | `ConversationListService`, `MailboxPollingService`, `MailAccountService` |
| `GET /chats/{key}` , `/chats/{key}/pane` | `ConversationController` | `ChatService.markRead`, conversation build |
| `POST /chats/{key}/send` (multipart; `body`,`subject`,`asReply`,`attachments`,`mailbox`) | `ConversationController` | `OutboundMessageService`, `ReplyService`, `PlanLimitService` |
| `GET/POST /chats/new`, `POST /chats/refresh` | `ConversationController` | same |
| `GET /chats/events` (SSE) | `ChatEventsController` | `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects to `/chats` |
| `/mailboxes` (list/new/create/`{id}/sync`/`{id}/signature`/`{id}/delete`/`preferences`) | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | `AccountController` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | admin metrics services |

## Shared model
`NavModelAdvice` / `SiteModelAdvice` add nav + site properties to every model.
`GlobalExceptionHandler` renders `error.html`.

## Known coupling to flag for mobile
`ConversationController` still holds orchestration (mailbox scope selection,
recipient resolution, signature appending, attachment conversion, new-vs-reply
decision in `send`). It must be extracted into a service before a REST API can
share it without duplication (see INTERNAL_TODO).
