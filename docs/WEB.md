# WEB: Thymeleaf website

Templates in `src/main/resources/templates`; shared pieces in `fragments/`
(e.g. `live-refresh.html`). Model attributes common to all pages come from
`NavModelAdvice` and `SiteModelAdvice`.

| Area | Routes | Controller |
|---|---|---|
| Marketing/public | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | PricingController, DemoController, LegalController, SeoController |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | PwaController |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email[/resend]` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController |
| Chats (main workspace) | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events` (SSE) | ConversationController, ChatEventsController |
| Legacy | `/threads/**`, `/senders` -> redirect to `/chats` | ChatRedirectController |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/{id}/sync|signature|delete`, `/preferences`, `/mailboxes/gmail/connect|reconnect/{id}|callback` | MailboxController, GmailConnectController |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/notifications|appearance|privacy|password|email` | AccountController |
| Billing | `/billing/checkout|portal|success|cancel|cancel-subscription|webhook` | BillingController, BillingWebhookController |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController |
| Digest | `/digest/opt-out` | DigestController |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController |

Services used by pages: chats -> `ChatService`, `ConversationListService`,
`ConversationService`, `ReplyService`, `OutboundMessageService`; mailboxes ->
`MailAccountService`; billing -> `BillingService`, `PlanLimitService`.
