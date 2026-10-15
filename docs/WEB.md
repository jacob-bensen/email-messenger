# Website (Thymeleaf) inventory

Templates in `src/main/resources/templates`; fragments in `fragments/`
(app-nav, chat-pane, live-refresh, settings-nav, seo, cookie/install banners,
sw-register).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing | DemoConversationService |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | UserService, OAuth2ProvisioningService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/verify-email[/resend]` | EmailVerificationController | verify-email | EmailVerificationService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account* | AccountService |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | ChatService, ConversationListService, ReplyService, OutboundMessageService, MailboxPollingService |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches/**` | SavedSearchController | (in chat) | SavedSearchService |
| `/billing/**` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite[/accept]` | TeamInviteController | team/* | TeamInviteService |
| `/digest/opt-out` | DigestController | digest/* | WeeklyDigestService |
| `/admin/**` | AdminRevenueController | admin/* | *MetricsService |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | — |
| `/robots.txt`, `/sitemap.xml` | SeoController | — | — |

Logic lives in the service layer; controllers (notably `ConversationController`,
which does recipient parsing, signature appending and mailbox selection) still
hold some orchestration that a REST controller would need to share — extract
before building `/api/v1` (INTERNAL_TODO #3).
