# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates/` (+ `fragments/`, `admin/`, `billing/`, `mailboxes/`, `password/`, `team/`, `digest/`).

| Route(s) | Controller | Template |
|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing |
| `/login`, `/register`, `/auth/google/start`, `/verify-email` | AuthController, OAuthStartController, EmailVerificationController | login, register, verify-email |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | (redirects) |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email}` | AccountController | account-* |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController | admin/* |
| `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml`, PWA assets, `/digest/opt-out`, `/threads/**` (redirect to /chats) | Legal/Seo/Pwa/Digest/ChatRedirect | legal, … |

Controllers resolve the user with `UserService.requireByEmail(principal)` and delegate to services (`ChatService`, `ConversationService`, `ReplyService`, `MailAccountService`, `PlanLimitService`…). A future `/api/v1` layer must reuse these services, not controllers.
