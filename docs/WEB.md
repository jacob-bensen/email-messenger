# WEB.md — Thymeleaf website (reverse-engineered 2026-11-07)

Templates: `src/main/resources/templates`. Fragments in `templates/fragments`.
Global model attributes via `NavModelAdvice`, `SiteModelAdvice`.

## Public
`/` and `/pricing` (PricingController → landing/pricing), `/demo` (DemoConversationService),
`/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`,
`/privacy`, `/terms`, `/refund`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated
| Route | Controller → services |
|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | ConversationController → ConversationListService, ChatService, MailAccountService |
| `POST /chats/{key}/send`, `/chats/new` | ReplyService, OutboundMessageService, PlanLimitService |
| `POST /chats/refresh` (JSON `{imported:n}`) | MailboxPollingService |
| `GET /chats/events` (SSE) | ChatEventStream |
| `/mailboxes/**`, `/mailboxes/gmail/**` | MailboxController, GmailConnectController → MailAccountService |
| `/searches` | SavedSearchController → SavedSearchService |
| `/account/**` | AccountController → AccountService |
| `/billing/**` | BillingController → BillingService |
| `/team/invite**` | TeamInviteController |
| `/admin/**` | AdminRevenueController (AdminAuthorizer) |

`/threads/**`, `/senders` redirect to `/chats` (ChatRedirectController).
