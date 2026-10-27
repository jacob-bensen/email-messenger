# Thymeleaf website

Templates in `src/main/resources/templates`; shared fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller |
|---|---|---|
| Marketing | `GET /`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` |
| Auth | `GET/POST /login`(Spring), `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `POST /verify-email/resend` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` |
| Chats (main app) | `GET /chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`; `POST /chats/new`, `/chats/refresh`, `/chats/{key}/send`; SSE `/chats/events`; legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` |
| Mailboxes | `/mailboxes` (list, `/new`, `POST` create, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`); Gmail `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `MailboxController`, `GmailConnectController` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` |
| Account | `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy`, `POST /account/password`, `/account/email` | `AccountController` |
| Billing | `POST /billing/checkout`, `/billing/portal`, `/billing/cancel-subscription`; `GET /billing/success`, `/billing/cancel`, `/billing/cancel-subscription` | `BillingController` |
| Teams | `/team/invite`, `/team/invite/accept` (GET+POST) | `TeamInviteController` |
| Digest | `GET /digest/opt-out` | `DigestController` |
| Admin | `GET /admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` |
| PWA/SEO | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | `PwaController`, `SeoController` |

Controllers delegate to services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, …); a future REST controller must reuse those.
