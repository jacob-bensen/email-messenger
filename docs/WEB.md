# WEB.md — Thymeleaf website (2026-10-30)

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public
`/` landing, `/pricing` (PricingController), `/demo` (DemoController → `DemoConversationService`),
`/privacy` `/terms` `/refund` (LegalController), `/login`, `/register`, `/password/forgot|reset`,
`/verify-email`, `/digest/opt-out`, `/billing/cancel`, `/robots.txt`, `/sitemap.xml`,
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated
| Route | Controller | Services |
|---|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane` | ConversationController | ConversationListService, ChatService, MailboxPollingService, BillingBannerService |
| `GET/POST /chats/new`, `POST /chats/{key}/send` | ConversationController | ReplyService, OutboundMessageService |
| `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | ConversationController, ChatEventsController | MailboxPollingService, ChatEventStream |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences` | MailboxController | MailAccountService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | GmailOAuthClient |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | AccountService |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController | BillingService |
| `/admin/revenue` (+ POST actions) | AdminRevenueController | admin metrics services |

Legacy `/threads/**`, `/senders` redirect to `/chats` (ChatRedirectController).
