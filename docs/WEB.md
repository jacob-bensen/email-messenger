# WEB (Thymeleaf website)

Templates: `src/main/resources/templates`. Shared fragments in
`templates/fragments/` (`app-nav`, `chat-pane`, `settings-nav`, `seo`,
`live-refresh`, `cookie-banner`, `install-banner`, `sw-register`).

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/` , `/pricing` | `PricingController` | `landing`, `pricing` |
| `/demo` | `DemoController` (`DemoConversationService`) | `chat` |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal` |
| `/login` `/register` `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` |
| `/password/forgot` `/password/reset` | `PasswordResetController` | `password/*` |
| `/verify-email` (+`/resend`) | `EmailVerificationController` | `verify-email` |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | `SeoController` | — |
| PWA: `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` | — |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats`, `/chats/{key}`, `/chats/{key}/pane` | `ConversationController` | `ConversationListService`, `ChatService`, `MailAccountService`, `MailboxPollingService` |
| `GET/POST /chats/new` | `ConversationController` | `OutboundMessageService` |
| `POST /chats/{key}/send` | `ConversationController` | `OutboundMessageService`, `ReplyService`, `PlanLimitService` |
| `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `MailboxPollingService`, `ChatEventStream` |
| `/threads/**`, `/senders` | `ChatRedirectController` | redirect to `/chats` |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences`) | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | `GmailConnectController` | `GmailOAuthClient` |
| `/searches` (+`/{id}/delete`) | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService`, `UserService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | `TeamInviteService` |
| `/admin/revenue` (+ POST actions) | `AdminRevenueController` | `*MetricsService` (gated by `AdminAuthorizer`) |
| `POST /billing/webhook` | `BillingWebhookController` | `BillingService` (Stripe-signed, public) |

## Known web/API-sharing gaps
See `INTERNAL_TODO.md` (mailbox scoping logic lives in the controller).
