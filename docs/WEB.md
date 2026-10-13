# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Model advice: `NavModelAdvice`, `SiteModelAdvice`.

## Public routes
`GET /` and `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email` (+ `POST /verify-email/resend`, auth), `/privacy`,
`/terms`, `/refund`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA assets
(`/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/apple-touch-icon.png`),
`POST /billing/webhook` (Stripe, CSRF-exempt), `GET /billing/cancel`.

## Authenticated routes
| Route | Controller → services |
|---|---|
| `GET /chats` (`q`,`mailbox`,`filter`,`page`) | ConversationController → ConversationListService, MailAccountService |
| `GET /chats/new`, `POST /chats/new`, `POST /chats/refresh` | ConversationController |
| `GET /chats/{key}`, `GET /chats/{key}/pane` | ConversationController → ChatService |
| `POST /chats/{key}/send` (multipart: body, subject, asReply, attachments) | ConversationController → ReplyService / OutboundMessageService |
| `GET /chats/events` (SSE) | ChatEventsController → ChatEventStream |
| `/threads/**`, `/senders` | ChatRedirectController (legacy redirects to /chats) |
| `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController → MailAccountService |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | GmailConnectController |
| `POST /searches`, `POST /searches/{id}/delete` | SavedSearchController |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email|appearance` | AccountController |
| `/billing/checkout|portal|success|cancel-subscription` | BillingController → BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | AdminRevenueController (AdminAuthorizer) |

Forms post with a CSRF token; errors render via `GlobalExceptionHandler` / `error.html`.
