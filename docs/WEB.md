# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments` (e.g. `chat-pane`, `live-refresh`). Global model advice: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler`, `error.html`.

## Public
`/` landing, `/pricing` (`PricingController`), `/demo` (`DemoConversationService`), `/login`, `/register`, `/password/forgot|reset`, `/verify-email`, `/privacy|/terms|/refund` (`LegalController`), `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` (`SeoController`), PWA: `/manifest.webmanifest`, `/icons/*`, `/sw.js`, `/offline` (`PwaController`), `/digest/opt-out`, `/billing/cancel`, `/auth/google/start`.

## Authenticated
| Route | Template | Services |
|---|---|---|
| `GET /chats` (`q`,`mailbox`,`filter`,`page`) | `chat` | `ConversationListService`, `MailAccountService`, `MailboxPollingService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `chat`, `fragments/chat-pane` | `ChatService` (markRead, conversation) |
| `GET/POST /chats/new` | `compose` | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` |
| `POST /chats/{key}/send` | redirect | `ReplyService`, `OutboundMessageService` |
| `POST /chats/refresh`, `GET /chats/events` | JSON / SSE | `MailboxPollingService`, `ChatEventStream` |
| `/mailboxes` (+`/new`,`/{id}/sync`,`/{id}/signature`,`/{id}/delete`,`/preferences`), `/mailboxes/gmail/*` | `mailboxes/*` | `MailAccountService`, `GmailOAuthClient` |
| `/searches` | — | `SavedSearchService` |
| `/account*` | `account*` | `AccountService`, `UserService` |
| `/billing/checkout|portal|success|cancel-subscription` | `billing/*` | `BillingService` |
| `/team/invite*` | `team/*` | `TeamInviteService` |
| `/admin/*` | `admin/*` | admin metrics services (`AdminAuthorizer`) |
| `/threads`, `/senders` | redirect to `/chats` | legacy `ChatRedirectController` |
