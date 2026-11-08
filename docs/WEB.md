# Website (Thymeleaf)

Templates: `src/main/resources/templates` (`fragments/` for nav, chat pane,
live-refresh, banners, SEO, service-worker registration). Static: `static/css`.

| Route | Auth | Controller | Notes |
|---|---|---|---|
| `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | public | Pricing/Demo/Legal/Seo | marketing + legal |
| `/login`, `/register`, `/auth/google/start` | public | AuthController, OAuthStartController | |
| `/password/forgot`, `/password/reset`, `/verify-email(/resend)` | public/auth | PasswordReset/EmailVerification | |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | user | ConversationController | texting-style workspace; query `q`, `filter`, `page`, `mailbox` |
| `POST /chats/{key}/send`, `POST /chats/new`, `POST /chats/refresh` | user | ConversationController | send reply / new mail / poll now |
| `GET /chats/events` | user | ChatEventsController | SSE new-mail events |
| `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync\|delete\|signature`, `/mailboxes/preferences`, `/mailboxes/gmail/*` | user | MailboxController, GmailConnectController | connect IMAP/Gmail |
| `/searches` | user | SavedSearchController | saved searches |
| `/account`, `/account/appearance\|notifications\|privacy`, `POST /account/email\|password` | user | AccountController | |
| `/billing/checkout\|portal\|success\|cancel\|cancel-subscription`, `POST /billing/webhook` | mixed | BillingController, BillingWebhookController | webhook is CSRF-exempt, signature-verified |
| `/team/invite(/accept)` | user | TeamInviteController | |
| `/digest/opt-out` | public | DigestController | |
| `/admin/revenue`, `/admin/retention/win-back` | admin | AdminRevenueController | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | PwaController | PWA shell |
| `/threads/**`, `/senders` | user | ChatRedirectController | legacy redirects to `/chats` |
