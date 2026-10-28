# WEB: Thymeleaf site (templates in `src/main/resources/templates`)

| Route | Controller | Template |
|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing |
| `/demo` | DemoController | (demo conversation) |
| `/login`, `/register`, `/verify-email*` | AuthController, EmailVerificationController | login, register, verify-email |
| `/password/forgot|reset` | PasswordResetController | password/* |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account*.html |
| `/chats` (list+filters), `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane |
| `/mailboxes`, `/mailboxes/new|{id}/sync|{id}/signature|{id}/delete|preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (fragment on chat) |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | BillingController, BillingWebhookController | billing/* |
| `/team/invite*` | TeamInviteController | team/* |
| `/digest/opt-out` | DigestController | digest/* |
| `/admin/revenue*` | AdminRevenueController (operator allowlist, 404 otherwise) | admin/* |
| `/privacy`, `/terms`, `/refund` | LegalController | legal |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | |
| `/robots.txt`, `/sitemap.xml` | SeoController | |

Shared fragments: `templates/fragments/` (e.g. `live-refresh.html`, `chat-pane.html`).
Services used by chat pages: `ChatService`, `ConversationListService`,
`MailAccountService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService`.
Caveat: `ConversationController` contains orchestration (mailbox scoping, signature
append, new-vs-reply decision in `send`) that is NOT in a service, so a mobile API
would currently have to duplicate it. See INTERNAL_TODO.md.
