# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (app-nav, chat-pane,
live-refresh, seo, settings-nav, cookie/install banners, sw-register).
Global model: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler` → `error.html`.

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, conversation | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | public / user |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send` | ConversationController | chat, compose, fragments/chat-pane | user |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats | user |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes[/{id}/sync\|/{id}/signature\|/{id}/delete\|/preferences]` | MailboxController | mailboxes/* | user |
| `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | GmailConnectController | – | user |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | – | user |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | AccountController | account* | user |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | user |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | mixed |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | AdminRevenueController | admin/revenue | admin emails |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | SeoController, PwaController | – | public |

Pages call the same service layer a future REST API must use; avoid logic in templates.
