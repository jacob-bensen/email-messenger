# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`. Layout reuse via `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). No shared layout dialect.

| Area | Routes | Controller | Templates |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController` (also serves `/`), `DemoController`, `LegalController` | landing, pricing, legal |
| Auth | `/login`, `/register`, `/password/*`, `/verify-email*`, `/auth/google/start` | `auth.*Controller` | login, register, password/*, verify-email |
| Chats workspace | `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | chat, compose, fragments/chat-pane |
| Mailboxes | `/mailboxes/**`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` | mailboxes/* |
| Saved searches | `/searches/**` | `SavedSearchController` | (in chat) |
| Account | `/account`, `/account/appearance|notifications|privacy|email|password`, `/preferences` | `AccountController` | account-* |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` | billing/* |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team/* |
| Admin | `/admin/revenue`, `/admin/retention/win-back`, … | `AdminRevenueController` | admin/revenue |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |

Controllers call services (`ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, `BillingService`, …); templates do not
contain business rules. Exception: orchestration in `ConversationController` (see BACKEND.md).
