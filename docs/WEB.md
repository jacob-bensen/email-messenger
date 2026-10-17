# Website (Thymeleaf) inventory

Templates in `src/main/resources/templates`. "Public" = `permitAll`.

| Route | Auth | Controller | Template | Services |
|---|---|---|---|---|
| `GET /` , `/pricing` | public | `PricingController` | `landing`, `pricing` | `LandingProperties`, `PlanPricing` |
| `GET /demo` | public | `DemoController` | `chat` (demo data) | `DemoConversationService` |
| `/privacy` `/terms` `/refund` | public | `LegalController` | `legal` | `LegalProperties` |
| `/login`, `/register` | public | `AuthController` | `login`, `register` | `UserService` |
| `/auth/google/start` | public | `OAuthStartController` | — | `OAuthIntentStore` |
| `/password/forgot`, `/password/reset` | public | `PasswordResetController` | `password/*` | `PasswordResetService` |
| `/verify-email`, `POST /verify-email/resend` | public / auth | `EmailVerificationController` | `verify-email` | `EmailVerificationService` |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | auth | `ConversationController` | `chat`, `compose`, `fragments/chat-pane` | `ConversationListService`, `ChatService`, `MailboxPollingService` |
| `POST /chats/{key}/send`, `/chats/new` | auth | `ConversationController` | — (redirect) | `ReplyService`, `OutboundMessageService` (+ logic in controller) |
| `POST /chats/refresh`, `GET /chats/events` | auth | `ConversationController`, `ChatEventsController` | JSON / SSE | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | auth | `ChatRedirectController` | redirect to `/chats` | — |
| `/mailboxes/**` (list, new, sync, signature, delete, preferences) | auth | `MailboxController` | `mailboxes/*` | `MailAccountService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | auth | `GmailConnectController` | — | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | auth | `SavedSearchController` | — | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy}`, `POST /account/{password,email}` | auth | `AccountController` | `account*` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | auth (cancel public) | `BillingController` | `billing/*` | `BillingService` |
| `POST /billing/webhook` | public (Stripe signature) | `BillingWebhookController` | — | `StripeWebhookGateway` |
| `/team/invite`, `/team/invite/accept` | auth | `TeamInviteController` | `team/*` | `TeamInviteService` |
| `/digest/opt-out` | public (token) | `DigestController` | `digest/*` | `WeeklyDigestService` |
| `/admin/revenue`, `POST /admin/revenue/reconcile-billing-period`, `POST /admin/retention/win-back` | admin | `AdminRevenueController` | `admin/*` | `*MetricsService` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | public | `PwaController` | — | — |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | `SeoController` | — | — |

Shared fragments live in `templates/fragments` (`chat-pane`, `live-refresh`, nav, etc.).
