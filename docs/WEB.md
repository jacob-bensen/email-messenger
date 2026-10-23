# Website (Thymeleaf)

Templates: `src/main/resources/templates/`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public routes
`/` landing, `/pricing`, `/demo`, `/login`, `/register`, `/password/forgot|reset`,
`/verify-email`, `/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/billing/cancel`,
`/robots.txt`, `/sitemap.xml`, PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated routes
| Route(s) | Controller | Services used |
|---|---|---|
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService`, `MailAccountService` |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/notifications|appearance|privacy|password|email` | `AccountController` | `AccountService` |
| `/billing/checkout|portal|success|cancel-subscription` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService`, `TeamService` |
| `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` | revenue / retention services |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirects to `/chats` |
