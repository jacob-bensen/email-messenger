# WEB.md — Thymeleaf website

Templates under `src/main/resources/templates`. Auth required unless "public".

| Route | Controller | Template | Services |
|---|---|---|---|
| `/` , `/pricing` (public) | `PricingController` | `landing`, `pricing` | `PlanLimits`, billing props |
| `/demo` (public) | `DemoController` | `chat` | `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` (public) | `AuthController`, `OAuthStartController` | `login`, `register` | `UserService`, OAuth |
| `/password/forgot`, `/password/reset` (public) | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` | `AccountService` |
| `/chats` (workspace list), `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` | `ConversationService`, `ReplyService`, `MailboxPollingService`, `MailAccountService` |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirect → `/chats` | — |
| `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `mailboxes/*` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | redirects | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | redirect | saved-search repo + `PlanLimitService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `POST /billing/webhook` (public, Stripe-signed) | `BillingController`, `BillingWebhookController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/digest/opt-out` (public) | `DigestController` | `digest/opt-out` | digest prefs |
| `/privacy`, `/terms`, `/refund` (public) | `LegalController` | `legal` | — |
| `/admin/revenue`, `POST /admin/revenue/*`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | `*MetricsService`, `AdminAuthorizer` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons (public) | `PwaController` | `fragments/sw-register` etc. | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` (public) | `SeoController` | — | — |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh` (SSE-driven in-place refresh),
`cookie-banner`, `install-banner`, `seo`, `sw-register`.
