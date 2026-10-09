# WEB.md — Thymeleaf website

All routes need an authenticated session unless marked *public*.

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/` *public*, `/pricing` *public*, `/demo` *public* | `PricingController`, `DemoController` | `landing`, `pricing`, `conversation` | `DemoConversationService`, `LandingProperties` |
| `/login`, `/register`, `/auth/google/start` *public* | `AuthController`, `OAuthStartController` | `login`, `register` | `UserService`, `OAuth2ProvisioningService` |
| `/password/forgot`, `/password/reset` *public* | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` (HTML fragment), `/chats/{key}/send`, `/chats/refresh` (JSON), `/chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane`, `fragments/live-refresh` | `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | `MailboxController` | `mailboxes/*` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | — | `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | — | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` | `AccountService` |
| `/billing/checkout`, `/portal`, `/success`, `/cancel`*public*, `/cancel-subscription` | `BillingController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/digest/opt-out` *public* | `DigestController` | `digest/opt-out` | `WeeklyDigestService` |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | admin metrics services |
| `/privacy`, `/terms`, `/refund` *public* | `LegalController` | `legal` | `LegalProperties` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` *public* | `SeoController` | — | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` *public* | `PwaController` | — | — |

Shared fragments: `fragments/{app-nav,settings-nav,seo,cookie-banner,install-banner,sw-register,live-refresh,chat-pane}`.
Global model attributes: `NavModelAdvice`, `SiteModelAdvice`.
