# WEB.md — Thymeleaf website (reverse-engineered 2026-10-16)

Templates: `src/main/resources/templates`. Shared fragments: `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Model advice: `NavModelAdvice`, `SiteModelAdvice`.

## Public routes (no auth)
`/` and `/pricing` (PricingController → landing/pricing), `/demo`, `/login`,
`/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`,
`/privacy`, `/terms`, `/refund`, `/billing/cancel`, `/billing/webhook` (Stripe),
`/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`,
`/sw.js`, `/offline`, `/icons/**`, `/actuator/health`.

## Authenticated routes
| Area | Routes | Controller → services |
|---|---|---|
| Chats | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/events` (SSE); `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send` | `ConversationController`, `ChatEventsController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes` (list/new/create, `{id}/sync`, `{id}/signature`, `{id}/delete`, `preferences`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` → `SavedSearchService` |
| Account | `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email|appearance` | `AccountController` → `AccountService` |
| Billing | `POST /billing/checkout|portal`, `/billing/success`, `/billing/cancel-subscription` | `BillingController` → `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| Legacy | `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` |

All POSTs require a CSRF token.
