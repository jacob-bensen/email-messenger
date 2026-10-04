# WEB.md — Thymeleaf website (inventory as of 2026-10-04)

Templates: `src/main/resources/templates/`. Fragments: `fragments/` (`app-nav`,
`chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`).

| Area | Routes | Controller (package) | Services |
|---|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `web` (Landing/Pricing/Demo/Legal/Seo) | `DemoConversationService` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `auth` | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats workspace | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`; `GET /chats/events` (SSE) | `web/ConversationController`, `ChatEventsController` | `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Legacy redirects | `/threads`, `/threads/**`, `/senders` | `web/ChatRedirectController` | — |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|delete|signature`, `/mailboxes/gmail/connect|callback|reconnect/{id}` | `web/MailboxController`, `GmailConnectController` | `MailAccountService` |
| Saved searches | `/searches/**` | `web/SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/{password,email,appearance,notifications,privacy}`, `/preferences` | `auth/AccountController` | `AccountService` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `/billing/webhook` | `billing` | `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `team` | `TeamInviteService` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `admin` | metrics services |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `web/PwaController` | — |

Known divergence risk: chat send/scoping logic is inside `ConversationController`
(private methods) — not yet reusable by an API controller.
