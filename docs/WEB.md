# Website (Thymeleaf)

Templates: `src/main/resources/templates`; shared fragments in
`templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`,
`seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model
attributes: `NavModelAdvice`, `SiteModelAdvice`.

## Public routes (no auth)
`/` (landing), `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/billing/cancel`, `/billing/webhook`, `/digest/opt-out`, `/robots.txt`,
`/sitemap.xml`, PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`.

## Authenticated routes
| Area | Routes | Controller |
|---|---|---|
| Chats | `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`; `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController` |
| Legacy | `/threads/**`, `/senders` → redirect | `ChatRedirectController` |
| Mailboxes | `/mailboxes/**` (connect, `{id}/sync`, `{id}/delete`, `{id}/signature`), `/mailboxes/gmail/**` (`connect`, `callback`, `reconnect/{id}`) | `MailboxController`, `GmailConnectController` |
| Account | `/account`, `/account/appearance|notifications|privacy|email|password`, `POST /preferences` | `AccountController` |
| Billing | `/billing/checkout|portal|success|cancel-subscription` | `BillingController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Searches | `/searches/**` | `SavedSearchController` |
| Digest | `/digest/**` | `DigestController` |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` |

Forms use CSRF tokens. Service usage per page: TBD (audit pending).
