# WEB (Thymeleaf site)

Templates in `src/main/resources/templates`; shared fragments in `fragments/`.

| Area | Routes | Template(s) |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | landing, pricing, legal |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | login, register, password/*, verify-email |
| Chats (core) | `/chats`, `/chats/new` (GET/POST), `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | chat, compose, fragments/chat-pane, live-refresh |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync\|signature\|delete`, `/mailboxes/preferences`, `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | mailboxes/* |
| Searches | `POST /searches`, `/searches/{id}/delete` | (in chat) |
| Account | `/account`, `/account/notifications\|appearance\|privacy\|password\|email` | account*.html |
| Billing | `POST /billing/checkout\|portal`, `/billing/success\|cancel\|cancel-subscription` | billing/* |
| Team | `/team/invite`, `/team/invite/accept` | team/* |
| Digest | `/digest/opt-out` | digest/opt-out |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | admin/revenue |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — |

Controllers are package-private and delegate to services (`ChatService`, `ConversationListService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, …).
Live updates: SSE `/chats/events` + `fragments/live-refresh.html`.
