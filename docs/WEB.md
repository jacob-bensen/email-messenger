# WEB (Thymeleaf site)

Templates: `src/main/resources/templates/`. Shared fragments in `fragments/` (app-nav, chat-pane, live-refresh, settings-nav, seo, cookie/install banners, sw-register).

| Route | Controller | Template | Services |
|---|---|---|---|
| `GET /`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | Landing/Pricing/Demo/Legal | landing, pricing, legal | DemoConversationService |
| `GET/POST /login`, `/register`, `/password/*`, `/verify-email*` | auth | login, register, password/*, verify-email | UserService, PasswordResetService, EmailVerificationService |
| `GET /chats` (+`?mailbox=`) | ConversationController | chat | ConversationListService, MailAccountService |
| `GET /chats/{key}`, `/chats/{key}/pane` | ConversationController | chat, fragments/chat-pane | ChatService (marks read) |
| `POST /chats/{key}/send` (multipart) | ConversationController | chat | OutboundMessageService, ReplyService |
| `GET/POST /chats/new` | ConversationController | compose | OutboundMessageService |
| `POST /chats/refresh` | ConversationController (JSON `{imported:n}`) | – | MailboxPollingService |
| `GET /chats/events` | ChatEventsController (SSE) | – | ChatEventStream |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches/**` | SavedSearchController | – | SavedSearchService |
| `/account/**`, `/billing/**`, `/team/invite*`, `/digest/opt-out` | account/billing/team/digest controllers | account*, billing/*, team/* | AccountService, BillingService, TeamInviteService |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/revenue | *MetricsService |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | – | – |

Only JSON-returning handler: `POST /chats/refresh`.
