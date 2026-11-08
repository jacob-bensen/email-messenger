# Website (Thymeleaf)

Templates in `src/main/resources/templates`, fragments in `templates/fragments`.
Public: `/`, `/pricing`, `/demo`, `/login`, `/register`, `/password/forgot|reset`, `/verify-email`,
`/privacy`, `/terms`, `/refund`, `/billing/cancel`, `/digest/opt-out`, PWA (`/manifest.webmanifest`,
`/sw.js`, `/offline`, `/icons/**`), SEO (`/robots.txt`, `/sitemap.xml`).

Authenticated:
| Route(s) | Controller | Service(s) |
|---|---|---|
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService`, `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|delete|signature`, `/preferences` | `MailboxController` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/appearance|notifications|privacy`, `/account/email|password` | `AccountController` | `AccountService` |
| `/billing/checkout|portal|success|cancel-subscription` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/admin/revenue`, `/admin/retention/win-back`, ... | `AdminRevenueController` | `admin.*` services |
