# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).
Global model advice: `NavModelAdvice`, `SiteModelAdvice`.

| Route(s) | Controller | Services / notes |
|---|---|---|
| `/`, `/demo` | `ChatRedirectController`/`DemoController` | landing; `DemoConversationService` |
| `/pricing` | `PricingController` | plan data |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | `UserService`, OIDC |
| `/password/forgot|reset` | `PasswordResetController` | `PasswordResetService` |
| `/verify-email`, `/verify-email/resend` | `EmailVerificationController` | `EmailVerificationService` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `/chats/refresh`, `/chats/{key}/send` | `ConversationController` | `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `/chats/events` (SSE) | `ChatEventsController` | `ChatEventStream` |
| `/threads`, `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirects to `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync|signature|delete`, `/preferences`) | `MailboxController` | `MailAccountService`, `PlanLimitService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | `GmailOAuthClient` |
| `/searches`, `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/appearance|notifications|privacy|email|password` | `AccountController` | `AccountService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | `BillingController`, `BillingWebhookController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/digest/opt-out` | `DigestController` | digest prefs |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | `AdminRevenueController` | admin metrics services |
| `/privacy`, `/terms`, `/refund` | `LegalController` | |
| `/robots.txt`, `/sitemap.xml` | `SeoController` | |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | |

Web/REST logic sharing audit: no REST layer exists, so no divergence is possible yet.
Some orchestration lives in controllers (`ConversationController.populateList`, account
colour maps, mailbox selection, `/chats/new` handling) and must move into services before a
JSON API can share it — tracked in `INTERNAL_TODO.md`.
