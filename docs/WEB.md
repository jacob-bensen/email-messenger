# Thymeleaf website

Templates in `src/main/resources/templates`. All controllers are package-private
classes in `web`, `auth`, `billing`, `team`, `digest`, `admin`.

## Public pages
`/` landing, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/login`,
`/register`, `/password/forgot`, `/password/reset`, `/verify-email`,
`/digest/opt-out`, `/billing/cancel`, `/offline`, plus `robots.txt`,
`sitemap.xml`, `manifest.webmanifest`, `sw.js`, icons, og-card.

## Authenticated pages
| Route | Template | Services |
|---|---|---|
| `GET /chats` | `chat` | `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `chat`, `fragments/chat-pane` | `ChatService` (markRead, conversation) |
| `GET/POST /chats/new` | `compose` | `OutboundMessageService` |
| `POST /chats/{key}/send` | redirect/`chat` | `OutboundMessageService`, `ReplyService` |
| `POST /chats/refresh` | JSON `{imported}` | `MailboxPollingService` |
| `GET /chats/events` | SSE | `ChatEventStream` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/mailboxes/gmail/*` | `mailboxes/*` | `MailAccountService`, Gmail OAuth |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `account*` | `AccountService`, `UserService` |
| `/searches` (POST, `/{id}/delete`) | redirect | `SavedSearchService` |
| `/team/invite`, `/team/invite/accept` | `team/*` | `TeamInviteService` |
| `/billing/{checkout,portal,success,cancel-subscription}` | `billing/*` | `BillingService` |
| `/admin/revenue` (+ POST reconcile/win-back) | `admin/revenue` | `*MetricsService` |
| `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` |

## Shared fragments
`app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE client),
`cookie-banner`, `install-banner`, `seo`, `sw-register`.
