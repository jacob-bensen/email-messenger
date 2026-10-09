# Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `fragments/`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`,
`cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | landing, pricing, legal |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot\|reset`, `/verify-email[/resend]` | `auth.*Controller` | login, register, password/*, verify-email |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | chat, compose, fragments/chat-pane |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync\|signature\|delete`, `/mailboxes/preferences`, `/mailboxes/gmail/connect\|reconnect/{id}\|callback` | `MailboxController`, `GmailConnectController` | mailboxes/* |
| Searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (inline in chat) |
| Account | `/account`, `/account/notifications\|appearance\|privacy\|password\|email` | `AccountController` | account*.html |
| Billing | `/billing/checkout\|portal\|success\|cancel\|cancel-subscription\|webhook` | `BillingController`, `BillingWebhookController` | billing/* |
| Teams | `/team/invite[/accept]` | `TeamInviteController` | team/* |
| Digest | `/digest/opt-out` | `DigestController` | digest/* |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | admin/* |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to `/chats` | `ChatRedirectController` | — |

Shared service layer: `ChatService`, `ConversationListService`,
`MailAccountService`, `OutboundMessageService`, `PlanLimitService`.
