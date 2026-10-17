# WEB.md — Thymeleaf website (reverse-engineered 2026-10-17)

Templates: `src/main/resources/templates/`. Fragments: `app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`, `sw-register`.

| Route | Auth | Controller | Template | Services |
|---|---|---|---|---|
| `/`, `/pricing` | public | `PricingController` | landing, pricing | — |
| `/demo` | public | `DemoController` | chat | `DemoConversationService` |
| `/login`, `/register` | public | `AuthController` | login, register | `UserService` |
| `/auth/google/start` | public | `OAuthStartController` | — | OAuth intent store |
| `/password/forgot|reset` | public | `PasswordResetController` | password/* | `PasswordResetService` |
| `/verify-email` (+`/resend` POST) | public/auth | `EmailVerificationController` | verify-email | `EmailVerificationService` |
| `/privacy`, `/terms`, `/refund` | public | `LegalController` | legal | — |
| `/chats` (list, `?q&mailbox&filter&page`) | auth | `ConversationController` | chat | `ConversationListService`, `MailboxPollingService` |
| `/chats/new` GET/POST | auth | same | compose | `ReplyService`, `OutboundMessageService` |
| `/chats/{key}` , `/chats/{key}/pane` | auth | same | chat, fragments/chat-pane | `ChatService` |
| `/chats/{key}/send` POST | auth | same | — | `ReplyService`, `OutboundMessageService` |
| `/chats/refresh` POST (JSON), `/chats/events` (SSE) | auth | same / `ChatEventsController` | — | polling, `ChatEventStream` |
| `/mailboxes` (+`/new`, `/{id}/sync|signature|delete`, `/preferences`) | auth | `MailboxController` | mailboxes/* | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | auth | `GmailConnectController` | — | `GmailOAuthClient` |
| `/searches` POST, `/searches/{id}/delete` | auth | `SavedSearchController` | — | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | auth | `AccountController` | account-* | `AccountService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | auth (cancel: public) | `BillingController` | billing/* | `BillingService` |
| `/billing/webhook` | public, Stripe signature | `BillingWebhookController` (@RestController) | — | `BillingService` |
| `/team/invite[/accept]` | auth | `TeamInviteController` | team/* | `TeamInviteService` |
| `/admin/revenue` (+POSTs) | operator | `AdminRevenueController` | admin/revenue | `*MetricsService` |
| `/digest/opt-out` | public | `DigestController` | digest/* | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | `PwaController` | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | — | — |
| `/threads/**`, `/senders` | auth | `ChatRedirectController` | — | redirect to `/chats` |
