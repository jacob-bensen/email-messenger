# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `app-nav`, `chat-pane`, `cookie-banner`, `install-banner`, `live-refresh`, `seo`, `settings-nav`, `sw-register`.

| Route | Auth | Controller | Template |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | public | PricingController, DemoController | landing, pricing, chat (demo data) |
| `/login`, `/register`, `/auth/google/start` | public | AuthController, OAuthStartController | login, register |
| `/password/forgot`, `/password/reset` | public | PasswordResetController | password/* |
| `/verify-email`, `/verify-email/resend` | public / auth | EmailVerificationController | verify-email |
| `/privacy`, `/terms`, `/refund` | public | LegalController | legal |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | auth | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane |
| `/threads`, `/threads/**`, `/senders` | auth | ChatRedirectController | redirect to /chats |
| `/mailboxes/*`, `/mailboxes/gmail/*` | auth | MailboxController, GmailConnectController | mailboxes/* |
| `/searches/{id}/delete` | auth | SavedSearchController | — |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | auth | AccountController | account-* |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | auth (`cancel` public) | BillingController | billing/* |
| `/billing/webhook` | public, Stripe signature | BillingWebhookController | — |
| `/team/invite`, `/team/invite/accept` | auth | TeamInviteController | team/* |
| `/digest/opt-out` | public | DigestController | digest/* |
| `/admin/revenue`, `/admin/*` POSTs | admin | AdminRevenueController | admin/* |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | public | PwaController, SeoController | generated inline |

Shared service layer: controllers call `UserService`, `ChatService`, `ConversationListService`, `MailAccountService`, `ReplyService`, `BillingService`, `PlanLimitService`. Any future REST controller must reuse these, not the controllers.
