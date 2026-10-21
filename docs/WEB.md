# Website (Thymeleaf)

Templates in `src/main/resources/templates`; shared fragments in `fragments/`
(`app-nav`, `chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Model advice: `NavModelAdvice`, `SiteModelAdvice`.

| Area | Routes | Controller | Services |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | `PricingController`, `DemoController`, `LegalController`, `SeoController` | `DemoConversationService` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `auth/*` | `UserService`, `PasswordResetService`, `EmailVerificationService` |
| Chats | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE); legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `ConversationListService`, `ChatService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `MailAccountService`, `PlanLimitService` |
| Searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | `BillingService` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team services |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/*Metrics*` |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | — |
| Digest | `/digest/opt-out` | `DigestController` | `WeeklyDigestService` |

Live updates: `fragments/live-refresh.html` subscribes to `/chats/events` and morphs
the rail and open pane in `chat.html`.
