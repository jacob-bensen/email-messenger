# WEB.md — Thymeleaf site & web app

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public (permit-all)
`/` and `/pricing` (`PricingController`), `/demo` (`DemoController`), `/login`,
`/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`,
`/privacy`, `/terms`, `/refund`, `/billing/cancel`, `/billing/webhook` (Stripe,
JSON, CSRF exempt), `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA assets
(`/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`), `/actuator/health`.

## Authenticated
| Route | Controller → services | Template |
|---|---|---|
| `GET /chats?page&q&mailbox&filter(ALL/UNREAD/AWAITING/ATTACHMENTS)` | `ConversationController` → `ConversationListService`, `MailAccountService`, `MailboxPollingService` | `chat.html` |
| `GET /chats/{key}`, `GET /chats/{key}/pane` | `ChatService` (marks read) | `chat.html`, `fragments/chat-pane` |
| `GET/POST /chats/new` | `OutboundMessageService` | `compose.html` |
| `POST /chats/{key}/send` (multipart: `body`, `subject`, `asReply`, `attachments[]`, `mailbox`) | `OutboundMessageService`/`ReplyService` | redirect |
| `POST /chats/refresh` → `{"imported":n}` (JSON) | `MailboxPollingService.refreshActiveUserNow` | — |
| `GET /chats/events` (SSE) | `ChatEventStream` | — |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences` | `MailboxController` → `MailAccountService` | `mailboxes/*` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | redirects |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | — |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{appearance,password,email}` | `AccountController` | `account*.html` |
| `POST /billing/checkout|portal|cancel-subscription`, `GET /billing/success` | `BillingController` | `billing/*` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| `/admin/revenue` (+2 POSTs) | `AdminRevenueController` | `admin/revenue.html` |

Legacy `/threads/**`, `/senders` redirect to `/chats`. `{key}` is the
participant-set `conversationKey`.
