# WEB.md — Thymeleaf website (2026-11-05)

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes (controller) | Template(s) | Services |
|---|---|---|---|
| Public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` (`PricingController`, `DemoController`, `LegalController`, `SeoController`) | landing, pricing, legal | `DemoConversationService`, `Landing/Site/LegalProperties` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | login, register, password/*, verify-email | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh` (returns JSON `{imported:int}`), `/chats/{key}/send`; SSE `/chats/events` | chat, compose, fragments/chat-pane | `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to `/chats` (`ChatRedirectController`) | — | — |
| Mailboxes | `/mailboxes` (list/new/create), `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`; `/mailboxes/gmail/{connect,reconnect/{id},callback}` | mailboxes/* | `MailAccountService`, `GmailOAuthClient` |
| Searches | `/searches` (`SavedSearchController`) | in chat | `SavedSearchService` |
| Account | `/account`, `/account/{appearance,notifications,privacy}`, POST `/account/{email,password,appearance}` | account* | `AccountService` |
| Billing | `POST /billing/{checkout,portal,cancel-subscription}`, `/billing/{success,cancel}`, `POST /billing/webhook` (Stripe, JSON, CSRF-exempt) | billing/* | `BillingService` |
| Team | `/team/invite`, `/team/invite/accept` | team/* | `TeamInviteService` |
| Admin | `/admin/revenue`, POST `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin/revenue | `*MetricsService` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` (`PwaController`) | — | — |
