# WEB.md — Thymeleaf website

All routes are session-authenticated unless marked *public*. POSTs require a CSRF token.

| Route(s) | Controller | Template | Notes |
|---|---|---|---|
| `/` (public), `/pricing`, `/demo` | PricingController, DemoController | landing, pricing | `DemoConversationService` |
| `/privacy` `/terms` `/refund` | LegalController | legal | public |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | SeoController | — | public |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` | PwaController | offline | PWA shell |
| `/login` `/register` `/auth/google/start` | AuthController, OAuthStartController | login, register | |
| `/verify-email`, `/verify-email/resend` | EmailVerificationController | verify-email | |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* | |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `/chats/new`, `POST /chats/refresh` | ConversationController | chat, compose, fragments/chat-pane | `?mailbox=`, `?q=`, `?filter=`, `?page=`; opening marks read |
| `/chats/events` | ChatEventsController | SSE | `NewMailEvent` stream |
| `/threads/**`, `/senders` | ChatRedirectController | — | legacy redirects to `/chats` |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete` | MailboxController | mailboxes/* | |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | GmailConnectController | — | Gmail OAuth |
| `POST /searches`, `/searches/{id}/delete` | SavedSearchController | — | |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email,appearance}` | AccountController | account-* | |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | team/* | |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | billing/* | Stripe |
| `POST /billing/webhook` | BillingWebhookController | — | JSON, no CSRF, signature-checked |
| `/digest/opt-out` | DigestController | digest/opt-out | |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin/revenue | `AdminAuthorizer` |

Shared fragments: `fragments/{app-nav,settings-nav,chat-pane,live-refresh,seo,cookie-banner,install-banner,sw-register}`.
Model advice: `NavModelAdvice`, `SiteModelAdvice`; errors: `GlobalExceptionHandler` + `error.html`.
