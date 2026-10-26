# Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model: `NavModelAdvice`, `SiteModelAdvice`.

## Public
`/` and `/pricing` (`PricingController`, landing/pricing), `/demo` (`DemoController`, `DemoConversationService`), `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated
- Chats (`ConversationController`): `GET /chats`, `/chats/new` (GET/POST), `POST /chats/refresh`, `GET /chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`; legacy `/threads/**`, `/senders` redirect (`ChatRedirectController`). Services: `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`.
- Mailboxes (`MailboxController`, `GmailConnectController`): `/mailboxes` (+ `/new`, `/{id}/sync|signature|delete`, `/preferences`), `/mailboxes/gmail/connect|reconnect/{id}|callback`. Service: `MailAccountService`.
- Saved searches: `POST /searches`, `/searches/{id}/delete`.
- Account: `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email|appearance`.
- Billing: `POST /billing/checkout|portal|cancel-subscription`, `GET /billing/success|cancel|cancel-subscription`.
- Teams: `/team/invite`, `/team/invite/accept`.
- Admin: `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back`.
