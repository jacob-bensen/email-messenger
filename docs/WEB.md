# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/{app-nav,chat-pane,cookie-banner,install-banner,live-refresh,seo,settings-nav,sw-register}`.

| Route(s) | Template | Controller |
|---|---|---|
| `/`, `/pricing` | landing, pricing | `PricingController` |
| `/demo` | (demo chat) | `DemoController` |
| `/privacy` `/terms` `/refund` | legal | `LegalController` |
| `/login` `/register` `/auth/google/start` | login, register | `AuthController`, `OAuthStartController` |
| `/password/forgot` `/password/reset` | password/* | `PasswordResetController` |
| `/verify-email` | verify-email | `EmailVerificationController` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send` | chat, compose | `ConversationController` (uses `ChatService`, `ReplyService`, `MailAccountService`) |
| `/mailboxes/**`, `/mailboxes/gmail/**` | mailboxes/* | `MailboxController`, `GmailConnectController` |
| `/searches/**` | (redirects) | `SavedSearchController` |
| `/account/**` | account-* | `AccountController` |
| `/billing/**` | billing/* | `BillingController` |
| `/team/invite/**` | team/* | `TeamInviteController` |
| `/digest/opt-out` | digest/opt-out | `DigestController` |
| `/admin/revenue` | admin/revenue | `AdminRevenueController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, og image | SEO | `SeoController` |
| `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` |
