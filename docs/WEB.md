# WEB — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(app-nav, chat-pane, settings-nav, live-refresh, seo, cookie-banner, install-banner, sw-register).

## Public (permit-all)
`/` and `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy|/terms|/refund` (LegalController), `/billing/cancel`,
`/billing/webhook`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA assets (`/manifest.webmanifest`,
`/icons/**`, `/sw.js`, `/offline`), static `/css|js|images`, `/actuator/health`.

## Authenticated
| Route | Controller / services |
|---|---|
| `GET /chats`, `/chats/new` (GET/POST), `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` | ConversationController / ChatService, ConversationListService, ReplyService, OutboundMessageService, MailboxPollingService |
| `/threads/**`, `/senders` | redirect to `/chats` (ChatRedirectController) |
| `/mailboxes` (+ `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController / MailAccountService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController / GmailOAuthClient |
| `/searches` (POST), `/searches/{id}/delete` | SavedSearchController / SavedSearchService |
| `/account`, `/account/notifications|appearance|privacy|password|email` | AccountController / AccountService |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController / BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController / TeamInviteService |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController (AdminAuthorizer) |

Forms POST with CSRF tokens. Pages and (future) API must share the service layer.
