# WEB — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/` (app-nav, settings-nav, chat-pane, live-refresh, seo, cookie/install banners, sw-register).

## Public routes
`/` and `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons.

## Authenticated routes
| Area | Routes | Controller |
|---|---|---|
| Chats | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send`; SSE `/chats/events` | `ConversationController`, `ChatEventsController` |
| Legacy | `/threads/**`, `/senders` redirect to `/chats` | `ChatRedirectController` |
| Mailboxes | `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` |
| Searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | `AccountController` |
| Billing | `POST /billing/checkout|portal`, `/billing/success|cancel|cancel-subscription` | `BillingController` |
| Team | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |

Services used by chats: `ChatService`, `ConversationListService`, `ConversationService`, `ReplyService`, `OutboundMessageService`, `SavedSearchService`.
