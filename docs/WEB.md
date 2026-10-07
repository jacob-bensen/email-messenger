# Website (Thymeleaf)

All pages are server-rendered from `src/main/resources/templates`. Controllers
are package-private `@Controller` classes; business logic lives in services.

| Area | Routes | Controller | Notes |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | public |
| SEO/PWA | `/robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `SeoController`, `PwaController` | public |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` | Spring Security form login + Google OIDC |
| Chats workspace | `/chats`, `/chats/new`, `/chats/refresh` (JSON), `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE); `/threads/**`, `/senders` redirect to `/chats` | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | uses `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| Mailboxes | `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `MailAccountService` |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription,webhook}` | `BillingController`, `BillingWebhookController` | Stripe |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | |
| Saved searches | `/searches`, `/searches/{id}/delete` | `SavedSearchController` | |
| Digest | `/digest/opt-out` | `DigestController` | |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | operator allowlist, 404 otherwise |

Shared fragments: `templates/fragments/` (`app-nav`, `chat-pane`,
`settings-nav`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`,
`sw-register`). Global model: `NavModelAdvice`, `SiteModelAdvice`.
