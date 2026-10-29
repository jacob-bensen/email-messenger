# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (app-nav, settings-nav,
chat-pane, live-refresh, seo, cookie-banner, install-banner, sw-register).

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing`, `/demo` | PricingController, DemoController | landing, pricing, chat | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | PasswordReset/EmailVerificationController | password/*, verify-email | public (resend: user) |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` (SSE) | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | user |
| `/threads/**`, `/senders` | ChatRedirectController | — (redirect to /chats) | user |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController | mailboxes/* | user |
| `POST /searches`, `POST /searches/{id}/delete` | SavedSearchController | — | user |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email}` | AccountController | account*.html | user |
| `/billing/checkout`, `/billing/portal`, `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription` | BillingController | billing/* | user (cancel: public) |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | user |
| `/digest/opt-out` | DigestController | digest/* | public |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | AdminRevenueController | admin/* | admin |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | PwaController, SeoController | — | public |

Admin role gating details: TBD (verify in `AdminRevenueController`).
