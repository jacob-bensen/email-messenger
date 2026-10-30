# Thymeleaf website

Templates in `src/main/resources/templates`; controllers in `web`, `auth`,
`billing`, `team`, `digest`, `admin`.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | BillingProperties/PlanPricing |
| `/demo` | DemoController | chat | DemoConversationService |
| `/login`, `/register` | AuthController | login, register | UserService |
| `/auth/google/start` | OAuthStartController | — | OAuthIntentStore |
| `/verify-email[/resend]` | EmailVerificationController | verify-email | EmailVerificationService |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | PasswordResetService |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | ConversationListService, ConversationService, ReplyService, OutboundMessageService, MailboxPollingService |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController | mailboxes/* | MailAccountService, GmailOAuthClient |
| `/searches/**` | SavedSearchController | (redirects) | saved-search repo |
| `/account/**` | AccountController | account*.html | AccountService |
| `/billing/**` | BillingController, BillingWebhookController | billing/* | BillingService |
| `/team/invite[/accept]` | TeamInviteController | team/* | team services |
| `/digest/opt-out` | DigestController | digest/opt-out | WeeklyDigestService |
| `/admin/revenue` | AdminRevenueController | admin/revenue | *MetricsService |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — | — |
| `/threads/**`, `/senders` | ChatRedirectController | — | redirect to `/chats` |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh`,
`install-banner`, `cookie-banner`, `seo`, `sw-register`.
