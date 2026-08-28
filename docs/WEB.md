# WEB.md — the ConexusMail Thymeleaf website

The backend serves the public + logged-in website via Thymeleaf. All
pages go through the same service layer as the (future) mobile REST API;
never fork business logic into a template or a controller.

## Page inventory

Grouped by controller. Every route below returns HTML unless marked
otherwise.

### Landing / marketing — `PricingController`, `SeoController`, `LegalController`, `DemoController`

| Method | Path            | Template            | Service |
| ------ | --------------- | ------------------- | ------- |
| GET    | `/`             | `landing.html`      | `LandingProperties`, `LandingVideo` |
| GET    | `/pricing`      | `pricing.html`      | `BillingProperties`, `PlanLimits`   |
| GET    | `/demo`         | `conversation.html` | `DemoConversationService`           |
| GET    | `/privacy`      | `legal.html`        | `LegalProperties`                   |
| GET    | `/terms`        | `legal.html`        | `LegalProperties`                   |
| GET    | `/refund`       | `legal.html`        | `LegalProperties`                   |
| GET    | `/robots.txt`   | (plain text)        | `SeoController`                     |
| GET    | `/sitemap.xml`  | (XML)               | `SeoController`                     |
| GET    | `/images/og-card.png` | (PNG)         | `SeoController`                     |

### Auth — `AuthController`, `EmailVerificationController`, `PasswordResetController`, `OAuthStartController`

| Method | Path                       | Template               | Service |
| ------ | -------------------------- | ---------------------- | ------- |
| GET    | `/login`                   | `login.html`           | (Spring Security form login POSTs to `/login`) |
| GET    | `/register`                | `register.html`        | `UserService`, `RegistrationForm` |
| POST   | `/register`                | (302 → verify-email)   | `UserService`, `EmailVerificationService` |
| GET    | `/verify-email`            | `verify-email.html`    | `EmailVerificationService` |
| POST   | `/verify-email/resend`     | (302)                  | `EmailVerificationService` |
| GET    | `/auth/google/start`       | (302 to Google)        | `OAuthIntentStore` |
| GET    | `/password/forgot`         | `password/forgot.html` | `PasswordResetService` |
| POST   | `/password/forgot`         | (302)                  | `PasswordResetService` |
| GET    | `/password/reset`          | `password/reset.html`  | `PasswordResetService` |
| POST   | `/password/reset`          | (302)                  | `PasswordResetService` |

### Account — `AccountController`

| Method | Path                        | Template                    | Service |
| ------ | --------------------------- | --------------------------- | ------- |
| GET    | `/account`                  | `account.html`              | `AccountService` |
| GET    | `/account/notifications`    | `account-notifications.html`| `AccountService` |
| GET    | `/account/appearance`       | `account-appearance.html`   | `AppearanceSettings` |
| POST   | `/account/appearance`       | (302)                       | `AppearanceSettings` |
| GET    | `/account/privacy`          | `account-privacy.html`      | `AccountService` |
| POST   | `/account/password`         | (302)                       | `UserService` |
| POST   | `/account/email`            | (302)                       | `UserService`, `EmailVerificationService` |

### Mailboxes — `MailboxController`, `GmailConnectController`

| Method | Path                              | Template                | Service |
| ------ | --------------------------------- | ----------------------- | ------- |
| GET    | `/mailboxes`                      | `mailboxes/list.html`   | `MailboxView` |
| GET    | `/mailboxes/new`                  | `mailboxes/new.html`    | `MailboxForm`, `MailboxProvider` |
| POST   | `/mailboxes`                      | (302)                   | mailbox connect service |
| POST   | `/mailboxes/{id}/sync`            | (302)                   | mailbox polling service |
| POST   | `/mailboxes/preferences`          | (302)                   | user mail preferences |
| POST   | `/mailboxes/{id}/signature`       | (302)                   | mailbox signature |
| POST   | `/mailboxes/{id}/delete`          | (302)                   | mailbox delete service |
| GET    | `/mailboxes/gmail/connect`        | (302 to Google)         | `GoogleOAuthProperties` |
| GET    | `/mailboxes/gmail/reconnect/{id}` | (302 to Google)         | `OAuth2ProvisioningService` |
| GET    | `/mailboxes/gmail/callback`       | (302)                   | `OAuth2ProvisioningService` |

### Chats — `ConversationController`, `ChatEventsController`, `ChatRedirectController`

| Method | Path                                | Template / Media              | Service |
| ------ | ----------------------------------- | ----------------------------- | ------- |
| GET    | `/chats`                            | `chat.html`                   | `ChatService`, `ConversationListService` |
| GET    | `/chats/new`                        | `compose.html`                | `ChatService` |
| POST   | `/chats/new`                        | (302)                         | `ChatService` |
| POST   | `/chats/refresh`                    | (302)                         | mailbox polling service |
| GET    | `/chats/{key}`                      | `chat.html`                   | `ConversationService`, `IMTransformService` |
| GET    | `/chats/{key}/pane`                 | `chat.html :: pane` fragment  | `ConversationService`, `IMTransformService` |
| POST   | `/chats/{key}/send`                 | (302)                         | `OutboundMessageService` → `ReplyService` |
| GET    | `/chats/events`                     | `text/event-stream` (`@ResponseBody`) | `ChatEventStream` |
| GET    | `/threads`, `/threads/**`, `/senders` | (302 → `/chats*`)           | `ChatRedirectController` |

### Search — `SavedSearchController`

| Method | Path                              | Template | Service |
| ------ | --------------------------------- | -------- | ------- |
| POST   | `/searches`                       | (302)    | `SavedSearchService` |
| POST   | `/searches/{id}/delete`           | (302)    | `SavedSearchService` |

### Billing — `BillingController`, `BillingWebhookController`

| Method | Path                              | Template / Media | Service |
| ------ | --------------------------------- | ---------------- | ------- |
| POST   | `/billing/checkout`               | (302 to Stripe)  | `BillingService`, `StripeCheckoutGatewayImpl` |
| POST   | `/billing/portal`                 | (302 to Stripe)  | `BillingService`, `StripePortalGatewayImpl` |
| GET    | `/billing/success`                | `billing/success.html` | `BillingService` |
| GET    | `/billing/cancel`                 | `billing/cancel.html`  | `BillingService` |
| GET    | `/billing/cancel-subscription`    | `billing/cancel-subscription.html` | `BillingService` |
| POST   | `/billing/cancel-subscription`    | (302)            | `BillingService`, `StripeSubscriptionGatewayImpl` |
| POST   | `/billing/webhook`                | `200 OK` JSON    | `StripeWebhookGatewayImpl` (`@RestController`) |

### Team — `TeamController` (in `team/`)

| Method | Path                              | Template            | Service |
| ------ | --------------------------------- | ------------------- | ------- |
| GET    | `/team/invite`                    | `team/invite.html`  | team invite service |
| POST   | `/team/invite`                    | (302)               | team invite service |
| GET    | `/team/invite/accept`             | `team/accept.html`  | team invite service |
| POST   | `/team/invite/accept`             | (302)               | team invite service |

### PWA + assets — `PwaController`

| Method | Path                                | Media |
| ------ | ----------------------------------- | ----- |
| GET    | `/manifest.webmanifest`             | `application/manifest+json` |
| GET    | `/icons/icon-192.png`               | `image/png` |
| GET    | `/icons/icon-512.png`               | `image/png` |
| GET    | `/icons/icon-512-maskable.png`      | `image/png` |
| GET    | `/apple-touch-icon.png`             | `image/png` |
| GET    | `/sw.js`                            | `application/javascript` |
| GET    | `/offline`                          | `text/html` |

### Admin — `AdminRevenueController`, `DigestController`

| Method | Path                                        | Template | Service |
| ------ | ------------------------------------------- | -------- | ------- |
| GET    | `/admin/revenue`                            | `admin/revenue.html` | revenue metrics services |
| POST   | `/admin/revenue/reconcile-billing-period`   | (302)    | `BillingPeriodBackfillService` |
| POST   | `/admin/retention/win-back`                 | (302)    | `WinBackOutreachService` |
| GET    | `/digest/opt-out`                           | `digest/opt-out.html` | `WeeklyDigestService` |

Admin routes are gated by `AdminAuthorizer` (email allowlist under
`admin.emails`); non-admins get 404.

## Shared fragments / layout

- `fragments/layout.html` — top-level shell (nav, footer, meta).
- `fragments/nav.html` — logged-in nav, driven by `NavModelAdvice` +
  `OnboardingChecklist`.
- `fragments/live-refresh.html` — wires SSE `/chats/events` into the
  chats pane (in-place morph on `NewMailEvent`).
- `fragments/billing-banner.html` — trial / dunning banner, driven by
  `BillingBannerService`.
- `fragments/site.html` — public site chrome.

Model attributes come from `@ControllerAdvice`s — `NavModelAdvice`,
`SiteModelAdvice`. Add controller-specific attributes via `Model`.

## Rule

Any URL under `/api/**` is reserved for the mobile REST API (see
`docs/API.md`). Do not add HTML routes under `/api`.
