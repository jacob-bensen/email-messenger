# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Fragments: `fragments/` (app-nav,
settings-nav, chat-pane, live-refresh, seo, cookie-banner, install-banner,
sw-register). Auditing date: 2026-11-07.

## Public pages
`/` and `/pricing` (PricingController), `/demo` (DemoController), `/login`,
`/register`, `/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`,
`/refund` (LegalController), `/digest/opt-out`, `/billing/cancel`.

## Authenticated pages
| Route | Controller | Services used |
|---|---|---|
| `GET /chats` | ConversationController | MailAccountService, ConversationListService, BillingBannerService |
| `GET /chats/new`, `POST /chats/new` | ConversationController | ConversationKeyService, OutboundMessageService |
| `GET /chats/{key}`, `/chats/{key}/pane` (HTML fragment) | ConversationController | ChatService |
| `POST /chats/{key}/send` | ConversationController | OutboundMessageService, ReplyService |
| `POST /chats/refresh`, `GET /chats/events` (SSE) | ConversationController, ChatEventsController | MailboxPollingService, ChatEventStream |
| `/threads/**`, `/senders` | ChatRedirectController | redirect to `/chats` |
| `/mailboxes` (+ `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController | MailAccountService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | AccountService |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController | BillingService |
| `/team/invite[/accept]` | TeamInviteController | TeamInviteService |
| `/admin/revenue`, `/admin/retention/win-back`, `…/reconcile-billing-period` | AdminRevenueController | admin metrics services (AdminAuthorizer) |

## Known coupling
`ConversationController` holds sizeable logic (mailbox selection, sending
account choice, signature appending, attachment conversion, as-reply thread
selection). It should move to services before any JSON API reuses it.
