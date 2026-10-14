# Thymeleaf website

All pages are server-rendered from `src/main/resources/templates`. Public routes are whitelisted in `SecurityConfig`.

| Route | Controller | Template | Services |
|---|---|---|---|
| `/` | `DemoController`/landing | `landing.html` | `DemoConversationService`, `SiteProperties` |
| `/demo` | `DemoController` | `chat.html` | `DemoConversationService` |
| `/pricing` | `PricingController` | `pricing.html` | `PlanPricing`, `BillingProperties` |
| `/login`, `/register` | `AuthController` | `login.html`, `register.html` | `UserService` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email` (+`/resend`) | `EmailVerificationController` | `verify-email.html` | `EmailVerificationService` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | `ConversationController` | `chat.html`, `compose.html`, `fragments/chat-pane.html` | `ConversationListService`, `ChatService`, `MailAccountService`, `MailboxPollingService` |
| `POST /chats/{key}/send`, `POST /chats/new` | `ConversationController` | — | `ReplyService`, `OutboundMessageService` |
| `/chats/events` (SSE), `POST /chats/refresh` | `ChatEventsController`/`ConversationController` | — | `ChatEventStream`, `MailboxPollingService` |
| `/threads`, `/senders` | `ChatRedirectController` | redirect to `/chats` | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/mailboxes/gmail/connect|callback|reconnect/{id}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` | `MailAccountService`, `GmailOAuthClient`, `PlanLimitService` |
| `/searches` | `SavedSearchController` | — | `SavedSearchService` |
| `/account`, `/account/{privacy,notifications,appearance,password,email}` | `AccountController`, `DigestController` | `account*.html` | `AccountService`, `UserService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/admin/revenue` (+POST actions) | `AdminRevenueController` | `admin/revenue.html` | `*MetricsService` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal.html` | `LegalProperties` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | `fragments/sw-register.html` | — |
| `/robots.txt`, `/sitemap.xml` | `SeoController` | — | — |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE → in-place morph), `cookie-banner`, `install-banner`, `seo`, `sw-register`.
