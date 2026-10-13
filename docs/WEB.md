# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments in `templates/fragments` (`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`, `install-banner`, `sw-register`).

| Area | Routes | Controller | Template(s) |
|---|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | `PricingController`, `DemoController`, `LegalController` | `landing`, `pricing`, `legal` |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`, `/verify-email/resend` | `AuthController`, `OAuthStartController`, `PasswordResetController`, `EmailVerificationController` | `login`, `register`, `password/*`, `verify-email` |
| Chats | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/refresh`, `/chats/events`; legacy `/threads/**`,`/senders` redirect | `ConversationController`, `ChatEventsController`, `ChatRedirectController` | `chat`, `compose`, `conversation`, `fragments/chat-pane` |
| Mailboxes | `/mailboxes`, `/mailboxes/new`, `/mailboxes/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `MailboxController`, `GmailConnectController` | `mailboxes/*` |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | (in chat) |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `account*` |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `billing/*` |
| Teams | `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `team/*` |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | `AdminRevenueController` | `admin/revenue` |
| Misc | `/digest/opt-out`, `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons) | `DigestController`, `SeoController`, `PwaController` | — |

Services used by chats: `ChatService`, `ConversationListService`, `ConversationService`, `OutboundMessageService`, `MailboxPollingService`.
