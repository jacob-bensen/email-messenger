# WEB — Thymeleaf website

Templates: `src/main/resources/templates/`; shared pieces in `fragments/`
(e.g. `fragments/live-refresh.html`, `fragments/chat-pane.html`).

| Route | Auth | Controller | Template |
|---|---|---|---|
| `/` | public | landing | `landing.html` |
| `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | public | `PricingController`, `DemoController`, `LegalController` | `pricing`, `chat`(demo), `legal` |
| `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email` | public | `auth.*Controller` | `login`, `register`, `password/*`, `verify-email` |
| `/chats` (q, mailbox, filter, page) | user | `ConversationController` | `chat.html` |
| `/chats/new` GET/POST | user | `ConversationController` | `compose.html` |
| `/chats/{key}` , `/chats/{key}/pane` (fragment) | user | `ConversationController` | `chat.html`, `fragments/chat-pane` |
| `POST /chats/{key}/send` (multipart; subject, asReply, attachments) | user | `ConversationController` | — |
| `POST /chats/refresh` → JSON `{"imported":n}` | user | `ConversationController` | — |
| `GET /chats/events` SSE `new-mail` | user | `ChatEventsController` | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences` | user | `MailboxController` | `mailboxes/*` |
| `/mailboxes/gmail/connect|callback|reconnect/{id}` | user | `GmailConnectController` | — |
| `/searches`, `/searches/{id}/delete` | user | `SavedSearchController` | — |
| `/account`, `/account/appearance|notifications|privacy`, POST email/password | user | `AccountController` | `account*.html` |
| `/billing/checkout|portal|success|cancel|cancel-subscription`, `/billing/webhook` | mixed | `BillingController`, `BillingWebhookController` | `billing/*` |
| `/team/invite`, `/team/invite/accept` | user | `TeamInviteController` | `team/*` |
| `/digest/opt-out` | public | `DigestController` | `digest/*` |
| `/admin/revenue` + POST actions | admin | `AdminRevenueController` | `admin/*` |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | public | `PwaController`, `SeoController` | — |

Services behind `/chats`: `ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, `ConversationService`.
