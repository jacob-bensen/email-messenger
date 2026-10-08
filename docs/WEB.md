# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `templates/fragments`.

| Area | Routes | Controller | Services |
|---|---|---|---|
| Marketing | `GET /`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `DemoConversationService` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`(+`/resend`) | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats (core) | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/{key}/send`, `/chats/refresh`; SSE `/chats/events` | `ConversationController`, `ChatEventsController` | `ConversationListService`, `ChatService`, `OutboundMessageService`, `ReplyService` |
| Legacy | `/threads/**`, `/senders` -> redirect to `/chats` | `ChatRedirectController` | — |
| Mailboxes | `/mailboxes` (+`/new`,`/{id}/sync|signature|delete`,`/preferences`), `/mailboxes/gmail/connect|reconnect/{id}|callback` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `MailboxPollingService` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email|appearance` | `AccountController` | `AccountService` |
| Billing | `POST /billing/checkout|portal|cancel-subscription`, `GET /billing/success|cancel|cancel-subscription` | `BillingController` | `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService`, `TeamService` |
| Admin | `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin.*MetricsService` |
| Digest | `GET /digest/opt-out` | `DigestController` | `WeeklyDigestService` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` | — |

Model advice: `NavModelAdvice` (authenticated nav state), `SiteModelAdvice` (site/OAuth availability).
