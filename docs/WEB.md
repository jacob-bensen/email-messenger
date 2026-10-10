# WEB — Thymeleaf website

Templates: `src/main/resources/templates`; fragments in `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

## Public
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/billing/cancel`, `/robots.txt`, `/sitemap.xml`, `/offline`.

## Authenticated
| Route | Controller → services |
|---|---|
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, POST `/chats/{key}/send`, `/chats/new`, `/chats/refresh` | `ConversationController` → `ChatService`, `ConversationListService`, `OutboundMessageService`, `ReplyService` |
| `/chats/events` (SSE) | `ChatEventsController` → `ChatEventStream` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches/**` | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/{appearance,notifications,privacy,email,password}` | `AccountController` → `AccountService` |
| `/billing/{checkout,portal,success,cancel-subscription}` | `BillingController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` → `TeamInviteService` |
| `/admin/revenue`, `/admin/retention/win-back`, reconcile | `AdminRevenueController` (admin only) |
| `/threads/**`, `/senders` | redirect to `/chats` (`ChatRedirectController`) |

Rule: controllers hold no business logic; add API endpoints through the same services.
