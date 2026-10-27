# Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/` (`app-nav`,
`chat-pane`, `live-refresh`, `settings-nav`, `seo`, `cookie-banner`, `install-banner`,
`sw-register`). Global model attributes: `NavModelAdvice`, `SiteModelAdvice`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/` | `PricingController` | `landing` |
| `/pricing` | `PricingController` | `pricing` |
| `/demo` | `DemoController` (`DemoConversationService`) | `conversation` |
| `/privacy` `/terms` `/refund` | `LegalController` | `legal` |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | `SeoController` | generated |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` | `PwaController` | PWA shell |
| `/digest/opt-out` | `DigestController` | `digest/opt-out` |

## Auth pages
`/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`,
`/verify-email` (+ `POST /verify-email/resend`), `/team/invite[/accept]`.
Controllers: `AuthController`, `OAuthStartController`, `PasswordResetController`,
`EmailVerificationController`, `TeamInviteController`.

## App pages (authenticated)
| Route | Controller | Services |
|---|---|---|
| `GET /chats` | `ConversationController` | `ConversationListService`, `MailAccountService`, `MailboxPollingService`, `BillingBannerService` |
| `GET /chats/{key}`, `/chats/{key}/pane` | `ConversationController` | `ChatService`, `ConversationKeyService` |
| `GET/POST /chats/new` | `ConversationController` | `OutboundMessageService` |
| `POST /chats/{key}/send` | `ConversationController` | `ReplyService`, `OutboundMessageService` |
| `POST /chats/refresh` (JSON) | `ConversationController` | `MailboxPollingService` |
| `GET /chats/events` (SSE) | `ChatEventsController` | `ChatEventStream` |
| `/mailboxes`, `/mailboxes/new`, `POST /mailboxes[/{id}/sync|signature|delete|preferences]` | `MailboxController` | `MailAccountService` |
| `/mailboxes/gmail/connect|reconnect/{id}|callback` | `GmailConnectController` | `GmailOAuthClient` |
| `POST /searches`, `/searches/{id}/delete` | `SavedSearchController` | saved-search repo |
| `/account`, `/account/notifications|appearance|privacy`, `POST /account/password|email` | `AccountController` | `AccountService` |
| `/billing/checkout|portal|success|cancel|cancel-subscription` | `BillingController` | `BillingService` |
| `/threads/**`, `/senders` | `ChatRedirectController` | legacy redirect to `/chats` |
| `/admin/revenue`, `POST /admin/retention/win-back`, `POST /admin/revenue/reconcile-billing-period` | `AdminRevenueController` (404 for non-operators) | `admin.*` metrics services |

Webhook: `POST /billing/webhook` (`BillingWebhookController`, Stripe signature, CSRF-exempt).
