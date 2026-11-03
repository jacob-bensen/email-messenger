# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `templates/fragments`.

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | PasswordReset/EmailVerification controllers | password/*, verify-email | public (resend: auth) |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/chats` (list, `q`,`mailbox`,`filter`,`page`) | ConversationController | chat | user |
| `/chats/new` GET/POST | ConversationController | compose | user |
| `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh` | ConversationController | chat, fragments/chat-pane | user |
| `/chats/events` (SSE) | ChatEventsController | – | user |
| `/mailboxes[...]`, `/mailboxes/gmail/*` | MailboxController, GmailConnectController | mailboxes/* | user |
| `/searches` | SavedSearchController | – | user |
| `/account[...]` | AccountController | account*.html | user |
| `/billing/*` | BillingController, BillingWebhookController | billing/* | mixed |
| `/team/invite[/accept]` | TeamInviteController | team/* | user |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/* | admin |
| `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons) | Digest/Seo/Pwa controllers | – | public |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` | user |

Services used by chat pages: ChatService, ConversationListService, ReplyService,
OutboundMessageService, MailboxPollingService, MailAccountService, ConversationKeyService,
BillingBannerService. Known divergence risk: see INTERNAL_TODO.md (send logic in controller).
