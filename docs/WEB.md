# WEB.md — Thymeleaf website (templates in `src/main/resources/templates`)

Public: `/` & `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund`, `/digest/opt-out`,
`/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons).

Authenticated:
- Chats (`ConversationController`): `GET /chats` (params page,q,mailbox,filter), `GET /chats/new`,
  `POST /chats/new`, `POST /chats/refresh`, `GET /chats/{key}`, `GET /chats/{key}/pane`,
  `POST /chats/{key}/send`, SSE `GET /chats/events` (`ChatEventsController`). Legacy `/threads/**`, `/senders` redirect.
  Template: `chat.html` (+ `fragments/live-refresh.html`).
- Mailboxes (`MailboxController`, `GmailConnectController`): `/mailboxes`, `/new`, `POST /{id}/sync|signature|delete`,
  `POST /preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback`.
- Saved searches: `POST /searches`, `POST /searches/{id}/delete`.
- Account: `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email`.
- Billing: `POST /billing/checkout|portal`, `/billing/success|cancel|cancel-subscription`.
- Team: `/team/invite`, `/team/invite/accept`.
- Admin: `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back`.

Shared services: `ConversationListService`, `ChatService`, `OutboundMessageService`, `ReplyService`,
`MailAccountService`, `PlanLimitService`. Known logic living in the controller: signature append
(`withSignature`), recipient parsing (`parseRecipients`), mailbox selection (`selectMailbox`) — see INTERNAL_TODO.
