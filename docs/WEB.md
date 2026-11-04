# WEB.md — Thymeleaf website (2026-11-04)

Templates in `src/main/resources/templates`; fragments in `fragments/`.

| Route(s) | Controller | Template | Auth |
|---|---|---|---|
| `/`, `/pricing` | `PricingController` | `landing`, `pricing` | public |
| `/demo` | `DemoController` | `chat` (demo data via `DemoConversationService`) | public |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal` | public |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | `SeoController` | – | public |
| PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | – | public |
| `/login` `/register` `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` | public |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` | public |
| `/verify-email`, `POST /verify-email/resend` | `EmailVerificationController` | `verify-email` | public / auth |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `chat`, `compose`, `fragments/chat-pane` | auth |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` | auth |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes[/{id}/sync\|signature\|delete]`, `POST /mailboxes/preferences` | `MailboxController` | `mailboxes/*` | auth |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | – | auth |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | – | auth |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{appearance,password,email}` | `AccountController` | `account*` | auth |
| `POST /billing/{checkout,portal,cancel-subscription}`, `/billing/{success,cancel,cancel-subscription}` | `BillingController` | `billing/*` | auth (cancel public) |
| `POST /billing/webhook` | `BillingWebhookController` (@RestController) | – | Stripe signature |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` | auth |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` | public |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` | admin (`AdminAuthorizer`) |

Shared fragments: `app-nav`, `settings-nav`, `chat-pane`, `live-refresh`
(SSE-driven in-place morph), `seo`, `cookie-banner`, `install-banner`,
`sw-register`. Global model attrs from `NavModelAdvice`, `SiteModelAdvice`.
