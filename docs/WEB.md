# WEB — Thymeleaf site

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (`app-nav`, `settings-nav`, `chat-pane`,
`live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). `NavModelAdvice` / `SiteModelAdvice` add global model data.

## Public pages
`/` and `/pricing` (PricingController, landing/pricing), `/demo` (DemoController → DemoConversationService),
`/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/billing/cancel`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (params `page,q,mailbox,filter`) | ConversationController | ConversationListService, MailboxPollingService, MailAccountService |
| `GET /chats/new`, `POST /chats/new` | ConversationController | OutboundMessageService / compose |
| `GET /chats/{key}`, `GET /chats/{key}/pane` | ConversationController | ChatService (markRead, buildFor) |
| `POST /chats/{key}/send` | ConversationController | OutboundMessageService, PlanLimitService |
| `POST /chats/refresh` (JSON `{"imported":n}`) | ConversationController | MailboxPollingService |
| `GET /chats/events` (SSE) | ChatEventsController | ChatEventStream |
| `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|signature|delete`, `/mailboxes/preferences` | MailboxController | MailAccountService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| `/account`, `/account/notifications|appearance|privacy|password|email` | AccountController | AccountService |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController | BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| `/admin/revenue` (+ POST reconcile / win-back) | AdminRevenueController | admin metrics services (AdminAuthorizer) |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats |
