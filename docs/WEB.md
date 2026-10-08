# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`.

| Route(s) | Controller | Template | Services / notes |
|---|---|---|---|
| `/`, `/pricing` | `PricingController` | `landing`, `pricing` | public; `LandingProperties` |
| `/demo` | `DemoController` | `chat` (demo data) | `DemoConversationService`, public |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal` | markdown in `resources/legal` |
| `/login` `/register` | `AuthController` | `login`, `register` | `UserService`; POST /login handled by Spring Security |
| `/auth/google/start` | `OAuthStartController` | — | Google OIDC, optional |
| `/password/forgot|reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email[/resend]` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/chats` | `ConversationController` | `chat` | `ConversationListService`, `MailboxPollingService` (async refresh); params `page,q,mailbox,filter` |
| `/chats/new` GET/POST | `ConversationController` | `compose` | `ReplyService.sendNewEmail`, `OutboundMessageService` |
| `/chats/{key}` , `/chats/{key}/pane` | `ConversationController` | `chat`, `fragments/chat-pane` | `ChatService.buildFor`, `markRead` |
| `/chats/{key}/send` POST | `ConversationController` | — | `ReplyService`, `OutboundMessageService`; multipart attachments |
| `/chats/refresh` POST | `ConversationController` | — | `MailboxPollingService.refreshActiveUserNow` |
| `/chats/events` | `ChatEventsController` | SSE | `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | — | legacy redirects |
| `/mailboxes[/new|/{id}/sync|/{id}/signature|/{id}/delete|/preferences]` | `MailboxController` | `mailboxes/*` | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | — | `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | — | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `billing/*` | Stripe |
| `/billing/webhook` | `BillingWebhookController` | — | Stripe signature, CSRF-exempt |
| `/team/invite[/accept]` | `TeamInviteController` | `team/*` | team services |
| `/digest/opt-out` | `DigestController` | `digest/*` | public token link |
| `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/*` | admin only |
| `/robots.txt` `/sitemap.xml` `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` | `SeoController`, `PwaController` | — | public |

Layout/shared: `fragments/` (incl. `live-refresh.html` for SSE-driven in-place updates in `chat.html`).
Errors: `GlobalExceptionHandler` → `error.html`.
