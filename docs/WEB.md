# WEB.md — Thymeleaf site

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (app-nav, settings-nav,
chat-pane, live-refresh, seo, cookie-banner, install-banner, sw-register).

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | PricingController | landing, pricing | public |
| `/demo` | DemoController | chat (demo data) | public |
| `/login`, `/register`, `/auth/google/start` | AuthController, OAuthStartController | login, register | public |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | public |
| `/verify-email` (+`/resend`) | EmailVerificationController | verify-email | public / user |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane | user |
| `/threads/**`, `/senders` | ChatRedirectController | redirect → /chats | user |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController | mailboxes/* | user |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | (redirects) | user |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | account*.html | user |
| `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | BillingController, BillingWebhookController | billing/* | mixed |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | user |
| `/digest/opt-out` | DigestController | digest/opt-out | public |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | AdminRevenueController | admin/revenue | operator allowlist |
| `/privacy`, `/terms`, `/refund` | LegalController | legal | public |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | SeoController, PwaController | — | public |

Controllers delegate to services; no business logic was found duplicated in templates (spot audit,
not exhaustive).
