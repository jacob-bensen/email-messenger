# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public pages
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/password/forgot`,
`/password/reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/billing/cancel`, `/digest/opt-out`, `/offline`, plus `robots.txt`,
`sitemap.xml`, `manifest.webmanifest`, `sw.js`, icons (PWA/SEO controllers).

## Authenticated pages
| Route | Controller → services |
|---|---|
| `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` | `ConversationController` → `ConversationListService`, `ChatService`, `UserService`, `OwnerAddressService` |
| `POST /chats/new`, `/chats/{key}/send` | `ConversationController` → `OutboundMessageService`, `ReplyService` |
| `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | `MailboxPollingService`, `ChatEventStream` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches/**` | `SavedSearchController` → `SavedSearchService` |
| `/account/**` | `AccountController` → `AccountService` |
| `/billing/**` | `BillingController` → `BillingService` |
| `/team/invite**` | `TeamInviteController` → `TeamInviteService` |
| `/admin/**` | `AdminRevenueController` → admin metric services |
| `/threads`, `/threads/**`, `/senders` | redirect to `/chats` (`ChatRedirectController`) |

Server webhooks: `POST /billing/webhook` (Stripe, JSON, signature header).
