# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Shared fragments in `fragments/`:
`app-nav`, `settings-nav`, `chat-pane` (open conversation pane; also served standalone),
`live-refresh` (SSE + in-place morph), `seo`, `cookie-banner`, `install-banner`, `sw-register`.

## Public routes
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot`, `/password/reset`, `/verify-email` (+`POST /verify-email/resend`),
`/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/billing/cancel`, `/robots.txt`,
`/sitemap.xml`, `/images/og-card.png`, PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated routes → controller → services
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter`) | `ConversationController` | `ConversationListService`, `MailboxPollingService`; template `chat` |
| `GET /chats/{key}` , `/chats/{key}/pane` | same | `ChatService.markRead/buildFor`; marks read |
| `GET/POST /chats/new` | same | compose; `ReplyService.sendNewEmail` + `OutboundMessageService.recordNewEmail` |
| `POST /chats/{key}/send` | same | multipart; `asReply` continues latest thread |
| `POST /chats/refresh` | same | JSON `{imported:n}` |
| `GET /chats/events` | `ChatEventsController` | SSE |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects to /chats |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | Gmail OAuth |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/appearance|password|email` | `AccountController` | |
| `POST /billing/checkout|portal|cancel-subscription`, `/billing/success`, `GET /billing/cancel-subscription` | `BillingController` | Stripe |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | admin allowlist |
