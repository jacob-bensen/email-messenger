# WEB.md — Thymeleaf website (reverse-engineered 2026-10-31)

Templates in `src/main/resources/templates`; shared pieces in `fragments/`.

| Route | Auth | Controller | Template |
|---|---|---|---|
| `GET /` | public | PricingController | landing.html |
| `GET /pricing`, `/demo` | public | PricingController / DemoController | pricing.html, chat |
| `/login`, `/register`, `/auth/google/start` | public | AuthController / OAuthStartController | login, register |
| `/password/forgot`, `/password/reset` | public | PasswordResetController | password/* |
| `/verify-email`, `POST /verify-email/resend` | public / auth | EmailVerificationController | verify-email |
| `/privacy`, `/terms`, `/refund` | public | LegalController | legal |
| `GET /chats` (params page,q,mailbox,filter) | auth | ConversationController | chat.html |
| `GET/POST /chats/new` | auth | ConversationController | compose.html |
| `POST /chats/refresh` | auth | ConversationController | redirect |
| `GET /chats/{key}`, `/chats/{key}/pane` | auth | ConversationController | chat.html / fragment |
| `POST /chats/{key}/send` (multipart) | auth | ConversationController | redirect |
| `GET /chats/events` (SSE) | auth | ChatEventsController | text/event-stream |
| `/mailboxes/new`, `POST /mailboxes/{id}/sync|signature|delete`, `POST /mailboxes/preferences` | auth | MailboxController | mailboxes/* |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | auth | GmailConnectController | redirect |
| `POST /searches/{id}/delete` | auth | SavedSearchController | redirect |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email|appearance` | auth | AccountController | account-*.html |
| `POST /billing/checkout|portal`, `/billing/success|cancel|cancel-subscription` | auth (cancel public) | BillingController | billing/* |
| `POST /billing/webhook` | public, Stripe-signed | BillingWebhookController | — |
| `/team/invite`, `/team/invite/accept` | auth | TeamInviteController | team/* |
| `/digest/opt-out` | public | DigestController | digest/* |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | admin | AdminRevenueController | admin/revenue |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | public | PwaController / SeoController | — |

Legacy `/threads`, `/senders` redirect to `/chats` (ChatRedirectController).
