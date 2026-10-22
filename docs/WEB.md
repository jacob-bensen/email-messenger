# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`). Global model attributes come from `SiteModelAdvice` / `NavModelAdvice`.

## Public routes
`/` and `/pricing` (PricingController), `/demo` (DemoService), `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`, `/privacy|terms|refund`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons).

## Authenticated routes
| Route | Controller | Notes |
|---|---|---|
| `GET /chats` (`page,q,mailbox,filter`), `/chats/new`, `POST /chats/new`, `POST /chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `GET /chats/events` (SSE) | ConversationController, ChatEventsController | Services: ConversationListService, ChatService, OutboundMessageService, MailboxPollingService |
| `/threads`, `/senders` | ChatRedirectController | legacy redirects to /chats |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController | MailAccountService |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | AccountController | |
| `POST /billing/checkout|portal|cancel-subscription`, `/billing/success|cancel`, `POST /billing/webhook` (public, Stripe-signed) | Billing* | |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | AdminAuthorizer |

All POSTs require a CSRF token.
