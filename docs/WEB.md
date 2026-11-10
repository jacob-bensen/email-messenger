# Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public
`/` landing (`PricingController`), `/pricing`, `/demo` (`DemoController`, `DemoConversationService`),
`/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`,
`/privacy`, `/terms`, `/refund` (`LegalController`), `/robots.txt`, `/sitemap.xml` (`SeoController`),
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` (`PwaController`).

## Authenticated
| Area | Routes | Service layer |
|---|---|---|
| Chats workspace | `GET /chats`, `/chats/{key}`, `/chats/{key}/pane` (fragment), `GET|POST /chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` (SSE); legacy `/threads/**`, `/senders` redirect to `/chats` | `ChatService`, `ConversationListService`, `MailboxPollingService`, `OutboundMessageService` |
| Mailboxes | `/mailboxes` (list/new/sync/preferences/signature/delete), `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailAccountService`, `GmailOAuthClient` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchService` |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountService` |
| Billing | `POST /billing/checkout|portal`, `/billing/success|cancel`, `/billing/cancel-subscription` | `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteService` |
| Digest | `/digest/opt-out` | `WeeklyDigestService` |
| Admin | `/admin/revenue` (+ reconcile, win-back POSTs) | `*MetricsService`, gated by `AdminAuthorizer` |

Only `chat.html` is a heavy single-page-like view: it morphs the rail and open pane in place
from SSE `new-mail` events (see `live-refresh.html`).
