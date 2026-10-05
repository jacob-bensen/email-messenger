# Thymeleaf website

Templates: `src/main/resources/templates/`. Shared fragments: `fragments/` (app-nav, chat-pane, settings-nav, live-refresh,
seo, cookie-banner, install-banner, sw-register).

| Route | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset`, `/verify-email` | PasswordReset/EmailVerificationController | password/*, verify-email | public |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | user |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* | user |
| `/searches/**` | SavedSearchController | (redirects) | user |
| `/account/**` | AccountController | account*.html | user |
| `/billing/**` | BillingController, BillingWebhookController | billing/* | user / webhook public |
| `/team/invite[/accept]` | TeamInviteController | team/* | user |
| `/digest/opt-out` | DigestController | digest/* | public |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/* | admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | — | public |

Services used by chat pages: ChatService, ConversationListService, ReplyService, OutboundMessageService,
MailboxPollingService, MailAccountService.
