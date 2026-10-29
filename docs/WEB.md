# Website (Thymeleaf) inventory

Templates in `src/main/resources/templates`; fragments in `templates/fragments`.
All routes below require login unless marked public. Verified from controller
mappings on 2026-10-29.

| Route(s) | Controller | Template | Services used |
|---|---|---|---|
| `/`, `/pricing`, `/demo` (public) | PricingController, DemoController | landing, pricing | DemoConversationService |
| `/login`, `/register` (public) | AuthController | login, register | UserService |
| `/password/forgot`, `/password/reset` (public) | PasswordResetController | password/* | PasswordResetService |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | EmailVerificationService |
| `/auth/google/start` (public) | OAuthStartController | — | OAuthIntentStore |
| `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send` | ConversationController | chat, compose | ConversationListService, ChatService, ReplyService, OutboundMessageService, MailboxPollingService |
| `/chats/events` (SSE) | ChatEventsController | — | ChatEventStream |
| `/threads/**`, `/senders` | ChatRedirectController | — | redirects to /chats |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | MailboxController | mailboxes/* | MailAccountService |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | — | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (fragment) | SavedSearchService |
| `/account`, `/account/notifications|appearance|privacy|password|email` | AccountController | account-* | AccountService |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | BillingController | billing/* | billing services |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | team services |
| `/digest/opt-out` (public) | DigestController | digest/* | digest services |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/* | admin metrics |
| `/privacy`, `/terms`, `/refund` (public) | LegalController | legal | — |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | SeoController, PwaController | — | — |

Shared model attributes come from `NavModelAdvice` and `SiteModelAdvice`.
