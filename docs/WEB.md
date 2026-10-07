# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).
Global model attributes: `NavModelAdvice`, `SiteModelAdvice`. Errors: `GlobalExceptionHandler` (HTML error page + status).

| Route | Template | Controller → services | Auth |
|---|---|---|---|
| `/` , `/pricing` | landing, pricing | `PricingController` | public |
| `/demo` | chat (demo data) | `DemoController` → `DemoConversationService` | public |
| `/login`, `/register`, `/auth/google/start` | login, register | `AuthController`, `OAuthStartController`, `UserService` | public |
| `/password/forgot`, `/password/reset` | password/* | `PasswordResetController` | public |
| `/verify-email`, `POST /verify-email/resend` | verify-email | `EmailVerificationController` | public / auth |
| `/privacy`,`/terms`,`/refund` | legal | `LegalController` | public |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane` | chat, fragments/chat-pane | `ConversationController` → `ConversationListService`, `ChatService`, `BillingBannerService` | auth |
| `/chats/new` (GET/POST), `/chats/{key}/send` | compose, chat | `ReplyService`, `OutboundMessageService`, `ConversationKeyService` | auth |
| `/chats/refresh` (POST, JSON `{imported:n}`), `/chats/events` (SSE) | — | `MailboxPollingService`, `ChatEventStream` | auth |
| `/mailboxes`, `/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/gmail/*` | mailboxes/* | `MailboxController`, `GmailConnectController`, `MailAccountService` | auth |
| `/searches`, `/searches/{id}/delete` | (redirects) | `SavedSearchController` | auth |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | account-* | `AccountController`, `AccountService` | auth |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`, `/cancel-subscription`, `/webhook` | billing/* | `BillingController`, `BillingWebhookController` | mixed |
| `/team/invite`, `/team/invite/accept` | team/* | `TeamInviteController` | auth |
| `/digest/opt-out` | digest/opt-out | `DigestController` | public |
| `/admin/revenue` + POSTs | admin/revenue | `AdminRevenueController` | operator allowlist |
| PWA/SEO: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**`, `/robots.txt`, `/sitemap.xml` | — | `PwaController`, `SeoController` | public |
| `/threads/**`, `/senders` | redirect to /chats | `ChatRedirectController` | auth |
