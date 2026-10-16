# Thymeleaf website

Templates: `src/main/resources/templates`. Shared model attributes via `NavModelAdvice`,
`SiteModelAdvice`; errors via `GlobalExceptionHandler` → `error.html`.

## Public routes
`/` and `/pricing` (PricingController), `/demo` (DemoService data), `/login`, `/register`,
`/auth/google/start`, `/password/forgot|reset`, `/verify-email`, `/privacy` `/terms`
`/refund` (LegalController), `/digest/opt-out`, `/robots.txt` `/sitemap.xml`
`/images/og-card.png` (SeoController), PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`,
icons (PwaController), `/billing/cancel`.

## Authenticated routes
| Route(s) | Controller | Services used |
|---|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh` | ConversationController | ConversationListService, ChatService, ReplyService, OutboundMessageService, MailboxPollingService, ConversationKeyService (**send orchestration lives in the controller**) |
| `GET /chats/events` | ChatEventsController | ChatEventStream |
| `/mailboxes` (list/new/create/`{id}/sync`/`preferences`/`{id}/signature`/`{id}/delete`) | MailboxController | MailAccountService, MailboxPollingService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | GmailOAuthClient, MailAccountService |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | AccountController | AccountService |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController | BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin metrics services (AdminAuthorizer) |
| `/threads`, `/threads/**`, `/senders` | ChatRedirectController | redirect to /chats (legacy) |

Fragments: `templates/fragments/` (`chat-pane`, `live-refresh`, …). Chat DOM updates in
place via SSE + morph (see CLAUDE.md roadmap, live delivery part 1).
