# Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

## Public routes (permitAll)
`/` and `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/billing/cancel`, `/billing/webhook`, `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`,
PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/apple-touch-icon.png`.

## Authenticated routes
| Area | Routes | Controller -> services |
|---|---|---|
| Chats | `GET /chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`; `GET /chats/events` | `ConversationController` -> `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService`, `MailAccountService` |
| Legacy | `/threads/**`, `/senders` redirect to `/chats` | `ChatRedirectController` |
| Mailboxes | `/mailboxes` (+`/new`, `POST /`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` -> `MailAccountService` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications|appearance|privacy`, `POST /account/appearance|password|email` | `AccountController` |
| Billing | `POST /billing/checkout|portal`, `/billing/success`, `/billing/cancel-subscription` | `BillingController` -> `BillingService` |
| Team | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` (allowlist; others get 404) | `AdminRevenueController` |
