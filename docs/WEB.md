# WEB — Thymeleaf site

Templates: `src/main/resources/templates`; fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller → services |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` → `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync|delete|signature`, `/preferences`, `/mailboxes/gmail/connect|callback|reconnect/{id}` | `MailboxController`, `GmailConnectController` → `MailAccountService` |
| Searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account/billing | `/account[/appearance|notifications|privacy|email|password]`, `/billing/*` | `AccountController`, `BillingController` |
| Teams/digest | `/team/invite[/accept]`, `/digest/opt-out` | `TeamInviteController`, `DigestController` |
| Admin | `/admin/revenue`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` |
