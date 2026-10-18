# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `fragments/` (app-nav, settings-nav, chat-pane,
live-refresh, seo, cookie-banner, install-banner, sw-register).

| Route(s) | Template | Controller |
|---|---|---|
| `/`, `/demo` | landing, chat | `ConversationController`/`DemoController` |
| `/pricing` | pricing | `PricingController` |
| `/privacy` `/terms` `/refund` | legal | `LegalController` |
| `/login` `/register` `/verify-email` | login, register, verify-email | `AuthController`, `EmailVerificationController` |
| `/password/forgot` `/password/reset` | password/* | `PasswordResetController` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh` | chat, compose, fragments/chat-pane | `ConversationController` |
| `/chats/events` (SSE) | — | `ChatEventsController` |
| `/threads`, `/senders` | redirects | `ChatRedirectController` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | mailboxes/* | `MailboxController`, `GmailConnectController` |
| `/searches/**` | — | `SavedSearchController` |
| `/account/**` | account*.html | `AccountController` |
| `/billing/**` | billing/* | `BillingController`, `BillingWebhookController` |
| `/team/invite**` | team/* | `TeamInviteController` |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/admin/revenue`, `/admin/retention/win-back` | admin/revenue | `AdminRevenueController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, og-card | — | `SeoController` |
