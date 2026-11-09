# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments` (`app-nav`, `settings-nav`,
`chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Template | Controller → services |
|---|---|---|
| `/`, `/demo`, `/pricing`, `/privacy`, `/terms`, `/refund` | landing, pricing, legal | `PricingController`, `DemoController` (`DemoConversationService`), `LegalController` |
| `/login`, `/register`, `/password/forgot|reset`, `/verify-email`, `/auth/google/start` | login, register, password/*, verify-email | `AuthController`, `PasswordResetController`, `EmailVerificationController`, `OAuthStartController` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | chat, compose, fragments/chat-pane | `ConversationController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`; SSE via `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | — | `ChatRedirectController` (redirects to /chats) |
| `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync|signature|delete`, `/mailboxes/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | mailboxes/* | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches`, `/searches/{id}/delete` | (fragment) | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/appearance|notifications|privacy|email|password` | account*.html | `AccountController` → `AccountService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | billing/* | `BillingController` → `BillingService` |
| `/team/invite`, `/team/invite/accept` | team/* | `TeamInviteController` → `TeamInviteService` |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin/revenue | `AdminRevenueController` (allowlist) |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `SeoController`, `PwaController` |
