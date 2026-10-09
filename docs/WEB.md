# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates` (account*, admin/, billing/, chat, compose, conversation,
digest/, error, landing, legal, login, mailboxes/, password/, pricing, register, team/, verify-email,
fragments/).

| Area | Routes | Controller |
|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `LandingController`/`PricingController`/`DemoController`/`LegalController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email[/resend]` | `auth.*Controller` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to chats | `ChatRedirectController` |
| Mailboxes | `/mailboxes/*` (connect, delete, sync, signature), `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` |
| Searches | `/searches/*` | `SavedSearchController` |
| Account | `/account`, `/account/{appearance,notifications,privacy,email,password}` | `AccountController` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` |
| Team | `/team/invite[/accept]` | `TeamInviteController` |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` |
| Digest | `/digest/opt-out`, `/preferences` | `DigestController` |

Controllers call services (`web.ChatService`, `OutboundMessageService`, `service.*`); no business logic
should live in templates.
