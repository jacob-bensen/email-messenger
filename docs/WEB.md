# Website (Thymeleaf)

Templates: `src/main/resources/templates`; shared pieces in `fragments/` (incl.
`live-refresh.html` for SSE-driven in-place updates). Controllers in `com.emailmessenger.*`.

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` |
| Chats | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE) | `ConversationController`, `ChatEventsController` |
| Legacy | `/threads/**`, `/senders` → redirect to chats | `ChatRedirectController` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` |
| Searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/notifications`, `/appearance`, `/privacy`, `/password`, `/email` | `AccountController` |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` |

Controllers resolve the user via `UserService.requireByEmail(principal)` and delegate to
services (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailAccountService`, `MailboxPollingService`, ...).
