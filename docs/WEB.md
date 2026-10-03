# Website (Thymeleaf)

Templates: `src/main/resources/templates`; shared fragments in `templates/fragments`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`, `cookie-banner`,
`install-banner`, `sw-register`). Global model attributes come from
`NavModelAdvice` / `SiteModelAdvice`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/` | `DemoController`/landing | `landing` |
| `/pricing` | `PricingController` | `pricing` |
| `/demo` | `DemoController` (`DemoConversationService`) | `chats` demo |
| `/login`, `/register`, `/auth/google/start` | `AuthController`, `OAuthStartController` | `login`, `register` |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | `password/*` |
| `/verify-email` (+`POST /verify-email/resend`) | `EmailVerificationController` | `verify-email` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | - |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | `PwaController` | - |
| `/digest/opt-out` | `DigestController` | - |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats`, `/chats/new` (+POST), `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `GET /chats/events` (SSE) | `ConversationController`, `ChatEventsController` | `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService`, `ChatEventStream` |
| `/threads`, `/senders` | `ChatRedirectController` | redirect to `/chats` |
| `/mailboxes/new`, `/{id}/sync`, `/{id}/delete`, `/{id}/signature`, `/preferences` | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | `GmailOAuthClient` |
| `/searches/{id}/delete` | `SavedSearchController` | `SavedSearchService` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `AccountController` | `AccountService` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `BillingController` | `BillingService` |
| `/team/invite`, `/team/invite/accept` | `TeamInviteController` | team services |
| `/admin/revenue`, `/admin/*` | `AdminRevenueController` | `*MetricsService` |

`POST /billing/webhook` (`BillingWebhookController`, `@RestController`) is the
Stripe webhook, public + CSRF-exempt.
