# WEB — Thymeleaf website

Templates in `src/main/resources/templates`; fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing` | `PricingController` | `landing`, `pricing` | `BillingProperties`, `LandingProperties` |
| `/demo` | `DemoController` | `chat` (demo data) | `DemoConversationService` |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` | `UserService`, `OAuth2ProvisioningService` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email`(+`/resend`) | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` | `ConversationListService`, `ChatService`, `MailboxPollingService`, `OutboundMessageService`, `ReplyService` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | redirect → `/chats` | — |
| `/mailboxes`, `/mailboxes/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`, `/mailboxes/gmail/*` | `MailboxController`, `GmailConnectController` | `mailboxes/*` | `MailAccountService`, `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | (redirects) | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` | `AccountService` |
| `/billing/checkout,portal,success,cancel,cancel-subscription` | `BillingController` | `billing/*` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` | `WeeklyDigestService` |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | `*MetricsService`, `WinBackOutreachService` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` | `LegalProperties` |
| PWA / SEO | `PwaController`, `SeoController` | generated | — |

The workspace JS (in `chat.html`) uses `fetch` with `X-Requested-With: fetch` and the `_csrf` meta tags to swap panes in place.
