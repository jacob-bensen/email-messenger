# Thymeleaf website

Templates: `src/main/resources/templates` (landing, pricing, login/register,
`chat.html` + `fragments/*`, `mailboxes/*`, `account*.html`, `billing/*`, `team/*`,
`admin/*`, `legal.html`, `digest/*`).

## Routes
**Public:** `/`, `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/billing/cancel`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`).

**Authenticated:**
- Chats: `GET /chats` (list, query/filter/paging), `GET|POST /chats/new`,
  `GET /chats/{key}`, `GET /chats/{key}/pane` (HTML fragment),
  `POST /chats/{key}/send` (multipart), `POST /chats/refresh` (JSON `{imported}`),
  `GET /chats/events` (SSE). `/threads`, `/senders` redirect to `/chats`.
- Mailboxes: `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`,
  `/mailboxes/gmail/connect|reconnect/{id}|callback`.
- Searches: `POST /searches/{id}/delete`.
- Account: `/account`, `/account/notifications|appearance|privacy|password|email`.
- Billing: `POST /billing/checkout|portal`, `/billing/success`,
  `/billing/cancel-subscription`; `POST /billing/webhook` (Stripe, signature auth).
- Team: `/team/invite`, `/team/invite/accept`.
- Admin: `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`,
  `POST /admin/retention/win-back`.

## Services per page
Chats → `ConversationListService`, `ChatService`, `MailAccountService`,
`MailboxPollingService`, `ReplyService`, `OutboundMessageService`,
`BillingBannerService`. Mailboxes → `MailAccountService`, `PlanLimitService`.
Shared model advice: `NavModelAdvice`, `SiteModelAdvice`.
