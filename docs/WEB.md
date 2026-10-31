# Website (Thymeleaf)

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`,
`settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model: `NavModelAdvice`, `SiteModelAdvice`.

## Pages

| Route | Template | Controller | Notes |
|---|---|---|---|
| `/` | `landing` | `PricingController` | public |
| `/pricing` | `pricing` | `PricingController` | public |
| `/demo` | `chat` (demo data) | `DemoController` | public, `DemoConversationService` |
| `/login`, `/register` | `login`, `register` | `AuthController` | public |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` | public |
| `/verify-email` (+`/resend`) | `verify-email` | `EmailVerificationController` | |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` | public |
| `/chats` | `chat` | `ConversationController` | list + empty pane; params `page`, `q`, `mailbox`, `filter` |
| `/chats/{key}` | `chat` | `ConversationController` | open conversation; marks read |
| `/chats/{key}/pane` | `fragments/chat-pane` | `ConversationController` | HTML fragment |
| `/chats/new` (GET/POST) | `compose` | `ConversationController` | new conversation |
| `/chats/{key}/send` (POST) | redirect | `ConversationController` | multipart reply/new message |
| `/chats/refresh` (POST), `/chats/events` (SSE) | — | `ConversationController`, `ChatEventsController` | JSON / SSE |
| `/threads`, `/threads/**`, `/senders` | — | `ChatRedirectController` | legacy redirects to `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`) | `mailboxes/*` | `MailboxController` | |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | redirects | `GmailConnectController` | |
| `/searches` (POST), `/searches/{id}/delete` | redirect | `SavedSearchController` | |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `account*` | `AccountController` | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `billing/*` | `BillingController` | |
| `/billing/webhook` | — | `BillingWebhookController` | Stripe, CSRF-exempt |
| `/team/invite`, `/team/invite/accept` | `team/*` | `TeamInviteController` | |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` | public |
| `/admin/revenue` (+POST actions) | `admin/revenue` | `AdminRevenueController` | `AdminAuthorizer` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | — | `PwaController` | PWA shell |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | — | `SeoController` | |
