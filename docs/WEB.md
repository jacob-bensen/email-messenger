# WEB — Thymeleaf site (ConexusMail)

Server-rendered pages shipped by the same Spring Boot app that serves
the mobile API. Both surfaces call the same service layer; never fork
business logic between the web controllers and REST controllers.

## Layout

- Templates: `src/main/resources/templates/`
- Fragments: `src/main/resources/templates/fragments/`
- Static assets: `src/main/resources/static/` (CSS, JS, icons, PWA
  manifest, service worker)
- Controllers: `com.emailmessenger.web.*`, plus `auth.*`, `billing.*`,
  `admin.*`, `team.*`, `digest.*`, `activation.*`
- Global view-model injection: `web.NavModelAdvice`

## Public pages (no auth)

| Route                       | Template                | Controller / notes                        |
| --------------------------- | ----------------------- | ----------------------------------------- |
| `/`                         | `landing.html`          | Landing / marketing page (`SeoController`)|
| `/pricing`                  | `pricing.html`          | `PricingController`, plan grid + CTA      |
| `/demo`                     | (rendered from service) | `DemoController` — sandbox chat view      |
| `/login`                    | `login.html`            | `AuthController` — form + Google button   |
| `/register`                 | `register.html`         | `AuthController`                          |
| `/password/forgot`          | `password/…`            | `PasswordResetController`                 |
| `/password/reset`           | `password/…`            | `PasswordResetController`                 |
| `/verify-email`             | `verify-email.html`     | `EmailVerificationController`             |
| `/privacy`, `/terms`, `/refund` | `legal.html`        | `LegalController`, sources under `resources/legal/` |
| `/digest/opt-out`           | `digest/…`              | `DigestController`                        |
| `/robots.txt`, `/sitemap.xml` |                        | `SeoController`                           |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` |     | `PwaController`                           |

## Authenticated app

| Route                             | Template                | Controller                    |
| --------------------------------- | ----------------------- | ----------------------------- |
| `/chats`, `/chats/{key}`          | `chat.html`             | `ConversationController`, uses `ChatService`, `ConversationListService` |
| `/chats/events` (SSE)             | —                       | `ChatEventsController` → `ChatEventStream`, `NewMailEvent` |
| `/compose`                        | `compose.html`          | `ConversationController` (compose flow) |
| `/mailboxes`, `/mailboxes/**`     | `mailboxes/*.html`      | `MailboxController`, `GmailConnectController` |
| `/saved-searches`                 | (fragment)              | `SavedSearchController`       |
| `/account`, `/account/**`         | `account*.html`         | `AccountController`, `AccountService`, `AccountBillingSummary` |
| `/billing/**`                     | `billing/*.html`        | `BillingController`, `BillingWebhookController` (Stripe) |
| `/teams`, `/teams/**`             | `team/*.html`           | `team.*Controller`            |
| `/admin/revenue`                  | `admin/revenue.html`    | `AdminRevenueController` (gated by `ADMIN_EMAILS`) |

## Cross-cutting concerns

- `GlobalExceptionHandler` — friendly 413 for oversized replies, error
  page for 500.
- `NavModelAdvice` — injects the shared nav model into every page.
- `SecurityConfig` — request-matcher whitelist for public URLs, form
  login, remember-me, optional Google OIDC, CSRF on (H2 + Stripe
  webhook exempt).
- `LoginThrottleFilter` — throttles bad credentials before the auth
  filter sees them.
- `OnboardingService` / `OnboardingChecklist` — surfaces the setup
  checklist on `/chats` for new users.

## Shared fragments

`templates/fragments/`:

- `layout.html` — top nav, footer, PWA install prompt.
- `live-refresh.html` — SSE wiring on `/chats` (subscribes to
  `/chats/events`, morphs the rail + open pane in place).
- Others: onboarding, plan badges, mailbox chips.

## Rule: one service layer

If a piece of business logic is needed by both a Thymeleaf page and the
mobile API, put it in `service/` (or the feature package) and call it
from both. Never re-implement, and never let the web path and the API
path diverge — the same input MUST produce the same underlying state
change.
