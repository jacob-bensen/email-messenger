# Thymeleaf website

Public (no auth): `/` and `/pricing` (PricingController → landing/pricing), `/demo`, `/login`, `/register`,
`/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund` (LegalController), `/digest/opt-out`,
`/billing/cancel`, `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons).

Authenticated:

| Route | Controller → services | Template |
|---|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | ConversationController → ConversationListService, ChatService, MailAccountService | `chat`, `fragments/chat-pane`, `compose` |
| `POST /chats/{key}/send`, `POST /chats/new` | ConversationController → OutboundMessageService → ReplyService | redirect |
| `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | ConversationController / ChatEventsController → MailboxPollingService, ChatEventStream | – |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | MailboxController → MailAccountService | `mailboxes/*` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController → GmailOAuthClient | redirect |
| `/searches`, `/searches/{id}/delete` | SavedSearchController → SavedSearchService | redirect |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController → AccountService | `account*` |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController → BillingService | `billing/*` |
| `/team/invite`, `/team/invite/accept` | TeamInviteController → TeamInviteService | `team/*` |
| `/admin/revenue` (+POSTs) | AdminRevenueController (allow-listed emails, else 404) | `admin/revenue` |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController → redirect to `/chats` | – |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`. Model advices: `NavModelAdvice`, `SiteModelAdvice`.
