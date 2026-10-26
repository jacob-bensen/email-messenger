# WEB — Thymeleaf website

Templates: `src/main/resources/templates`; fragments in `templates/fragments`
(e.g. `live-refresh.html`, `chat-pane`). Static CSS/JS in `static/`.

## Public (no auth)
`/` and `/pricing` (PricingController), `/demo` (DemoService), `/login`, `/register`,
`/auth/google/start`, `/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund`
(LegalController), `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, `/images/og-card.png`
(SeoController), PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`.

## Authenticated
| Route | Controller → services |
|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane` | ConversationController → ConversationListService, ChatService, MailboxPollingService, BillingBannerService |
| `GET/POST /chats/new`, `POST /chats/{key}/send` | ConversationController → ReplyService, OutboundMessageService, ConversationKeyService |
| `POST /chats/refresh`, `GET /chats/events` (SSE) | ConversationController, ChatEventsController |
| `/mailboxes[/new|/{id}/sync|/{id}/signature|/{id}/delete|/preferences]` | MailboxController → MailAccountService, PlanLimitService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController → GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController → SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController → AccountService |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | BillingController → BillingService |
| `/team/invite[/accept]` | TeamInviteController → TeamInviteService |
| `/admin/revenue`, `/admin/retention/win-back` | AdminRevenueController (allowlisted) |
| `POST /billing/webhook` | Stripe webhook (signature-verified, CSRF-exempt) |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController (legacy redirect to /chats) |
