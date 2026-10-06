# WEB.md — Thymeleaf website (from code, 2026-10-06)

Templates: `src/main/resources/templates`. Fragments: `fragments/{app-nav,chat-pane,cookie-banner,install-banner,live-refresh,seo,settings-nav,sw-register}.html`.

| Route(s) | Template | Controller → services |
|---|---|---|
| `/` | landing | `ChatRedirectController`/landing; `SiteProperties` |
| `/pricing`, `/demo` | pricing, chat (demo) | `PricingController`, `DemoController` → `DemoConversationService` |
| `/login`, `/register`, `/password/forgot|reset`, `/verify-email[/resend]` | login, register, password/*, verify-email | `AuthController`, `PasswordResetController`, `EmailVerificationController` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` (GET/POST), `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE) | chat, compose, fragments/chat-pane | `ConversationController`, `ChatEventsController` → `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/threads`, `/threads/**`, `/senders` | — | legacy redirects (`ChatRedirectController`) |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/mailboxes/gmail/{connect,callback,reconnect/{id}}` | mailboxes/* | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| `/searches` (+`/{id}/delete`) | — | `SavedSearchController` → `SavedSearchService` |
| `/account`, `/account/{appearance,notifications,privacy,password,email,preferences}` | account*.html | `AccountController` → `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | billing/* | `BillingController`, `BillingWebhookController` → `BillingService` |
| `/team/invite[/accept]` | team/* | `TeamInviteController` |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin/revenue | `AdminRevenueController` |
| `/privacy`, `/terms`, `/refund` | legal | `LegalController` |
| `/sw.js`, `/manifest.webmanifest`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | — | `PwaController`, `SeoController` |
