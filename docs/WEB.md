# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model attributes come from `NavModelAdvice` and
`SiteModelAdvice`; errors from `GlobalExceptionHandler` → `error.html`.

## Public routes
`/` (landing, `PricingController`), `/pricing`, `/demo`, `/login`, `/register`,
`/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`,
`/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/billing/cancel`,
`/robots.txt`, `/sitemap.xml`, `/images/og-card.png`, PWA: `/manifest.webmanifest`,
`/icons/*`, `/apple-touch-icon.png`, `/sw.js`, `/offline`.

## Authenticated routes
| Area | Routes | Controller / services |
|---|---|---|
| Chats | `GET /chats` (`page`, `q`, `mailbox`, `filter`), `GET/POST /chats/new`, `GET /chats/{key}`, `GET /chats/{key}/pane`, `POST /chats/{key}/send` (multipart; `asReply`, `subject`, `attachments`), `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController`; `ConversationListService`, `ChatService`, `OutboundMessageService`, `MailboxPollingService` |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `POST /mailboxes/{id}/sync\|signature\|delete`, `POST /mailboxes/preferences`, `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | `MailboxController`, `GmailConnectController`; `MailAccountService` |
| Saved searches | `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications`, `/account/appearance` (GET/POST), `/account/privacy`, `POST /account/password`, `POST /account/email`, `POST /verify-email/resend` | `AccountController`, `EmailVerificationController` |
| Billing | `POST /billing/checkout`, `POST /billing/portal`, `/billing/success`, `/billing/cancel-subscription` (GET/POST) | `BillingController`; `BillingService` |
| Teams | `/team/invite` (GET/POST), `/team/invite/accept` (GET/POST) | `TeamInviteController` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` (guarded by `AdminAuthorizer`) |

Live updates: `fragments/live-refresh.html` opens the SSE stream and morphs the chat rail and
open pane in place.
