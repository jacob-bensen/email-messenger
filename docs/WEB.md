# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Fragments in `templates/fragments`.
All routes except the public list below require a session login (`SecurityConfig`).

## Public
`/` and `/pricing` (PricingController), `/demo`, `/login`, `/register`, `/auth/google/start`,
`/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
`/digest/opt-out`, `/billing/cancel`, `/billing/webhook` (Stripe, CSRF-exempt),
`/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons).

## Authenticated
| Area | Routes | Controller → services |
|---|---|---|
| Chats workspace | `GET /chats` (`q`, `mailbox`, `filter`, `page`), `GET/POST /chats/new`, `POST /chats/refresh` (JSON `{imported}`), `GET /chats/{key}`, `GET /chats/{key}/pane`, `POST /chats/{key}/send`, `GET /chats/events` (SSE) | `ConversationController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`; Gmail `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchService` |
| Account | `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | `AccountController` |
| Billing | `POST /billing/checkout|portal`, `/billing/success`, `/billing/cancel-subscription` | `BillingController` → `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| Legacy | `/threads/**`, `/senders` redirect to `/chats` | `ChatRedirectController` |

Layout/fragments: `fragments/chat-pane`, `fragments/live-refresh` (SSE morph).
