# Thymeleaf website

Templates: `src/main/resources/templates`; CSS/JS: `src/main/resources/static`.

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo` | `PricingController`, `DemoController` | `landing`, `pricing` |
| Legal/SEO/PWA | `/privacy` `/terms` `/refund`, `/robots.txt` `/sitemap.xml`, `/manifest.webmanifest` `/sw.js` `/offline` | `LegalController`, `SeoController`, `PwaController` | `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `auth.*Controller` | `login`, `register`, `password/*`, `verify-email` |
| Chats workspace | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` (fragment), `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` (JSON), `GET /chats/events` (SSE); `/threads/**`, `/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `chat`, `compose`, `fragments/chat-pane`, `fragments/live-refresh` |
| Mailboxes | `/mailboxes[/new|/{id}/sync|/{id}/signature|/{id}/delete|/preferences]`, `/mailboxes/gmail/{connect|reconnect/{id}|callback}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | (in `chat`) |
| Account | `/account`, `/account/{notifications|appearance|privacy|password|email}` | `AccountController` | `account*` |
| Billing | `/billing/{checkout|portal|success|cancel|cancel-subscription|webhook}` | `BillingController`, `BillingWebhookController` | `billing/*` |
| Teams | `/team/invite[/accept]` | `TeamInviteController` | `team/*` |
| Digest | `/digest/opt-out` | `DigestController` | `digest/*` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/*` |

Services used by chats: `ConversationListService`, `ChatService`, `ReplyService`,
`OutboundMessageService`, `MailboxPollingService`, `ConversationKeyService`.
