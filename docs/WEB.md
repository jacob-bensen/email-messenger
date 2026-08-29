# WEB — Thymeleaf pages

Server-rendered pages live in `src/main/resources/templates/`. Every
controller listed here shares its service layer with what will become
the JSON API — do not duplicate business logic in a template or a
controller helper (`docs/API.md`).

## Page inventory

### Marketing / legal

| Route            | Template          | Controller           |
|------------------|-------------------|----------------------|
| `/`              | `landing.html`    | `PricingController`  |
| `/pricing`       | `pricing.html`    | `PricingController`  |
| `/demo`          | (demo)            | `DemoController`     |
| `/privacy`       | `legal.html`      | `LegalController`    |
| `/terms`         | `legal.html`      | `LegalController`    |
| `/refund`        | `legal.html`      | `LegalController`    |

### Auth

| Route              | Template               | Controller               |
|--------------------|------------------------|--------------------------|
| `/login`           | `login.html`           | `AuthController`         |
| `/register`        | `register.html`        | `AuthController`         |
| `/verify-email`    | `verify-email.html`    | `EmailVerificationCtrl.` |
| `/password/forgot` | `password/forgot.html` | `PasswordResetController`|
| `/password/reset`  | `password/reset.html`  | `PasswordResetController`|

### Account

| Route                    | Template                     |
|--------------------------|------------------------------|
| `/account`               | `account.html`               |
| `/account/notifications` | `account-notifications.html` |
| `/account/appearance`    | `account-appearance.html`    |
| `/account/privacy`       | `account-privacy.html`       |

All under `AccountController`, service-backed by `AccountService`,
`UserService`, `AuthEventService`.

### Mailboxes

| Route                    | Template               | Controller             |
|--------------------------|------------------------|------------------------|
| `/mailboxes`             | `mailboxes/index.html` | `MailboxController`    |
| `/mailboxes/new`         | `mailboxes/new.html`   | `MailboxController`    |
| `/mailboxes/gmail/...`   | (redirects)            | `GmailConnectController` |

### Chats (the product)

| Route              | Template / fragment      | Controller               |
|--------------------|--------------------------|--------------------------|
| `/chats`           | `chat.html`              | `ConversationController` |
| `/chats/{key}`     | `chat.html`              | `ConversationController` |
| `/chats/{key}/pane`| `fragments/chat-pane`    | `ConversationController` |
| `/chats/new`       | `compose.html`           | `ConversationController` |
| `/chats/events`    | (SSE)                    | `ChatEventsController`   |

`chat.html` uses HTMX + `fragments/live-refresh.html` to consume the
SSE stream and morph the rail / open pane in place.

### Billing

| Route                            | Template / behaviour              |
|----------------------------------|-----------------------------------|
| `POST /billing/checkout`         | redirect to Stripe                |
| `POST /billing/portal`           | redirect to Stripe portal         |
| `GET  /billing/success`          | `billing/success.html`            |
| `GET  /billing/cancel`           | redirect to `/pricing`            |
| `GET  /billing/cancel-subscription` | `billing/cancel-subscription.html` |
| `POST /billing/cancel-subscription` | processes and redirects        |
| `POST /billing/webhook`          | Stripe webhook (JSON, HMAC)       |

### Team

`TeamInviteController`: `GET|POST /team/invite`,
`GET|POST /team/invite/accept` — uses templates under `team/`.

### Digest

`GET /digest/opt-out` (`DigestController`) — templates under `digest/`.

### Admin

`GET /admin/revenue` renders `admin/revenue.html`
(`AdminRevenueController`). Guarded by `AdminAuthorizer` which checks
the current session's user email against `admin.emails` config.

### PWA

`PwaController` serves `/manifest.webmanifest`, `/icons/*.png`,
`/apple-touch-icon.png`, `/sw.js`, and `/offline`. The service worker
+ manifest turn the site into an installable PWA.

## Shared fragments (`templates/fragments/`)

- `app-nav.html`, `settings-nav.html` — top / side navigation.
- `chat-pane.html` — the single-conversation pane, re-rendered
  standalone for HTMX partials and SSE morphing.
- `live-refresh.html` — HTMX SSE plumbing.
- `install-banner.html`, `cookie-banner.html` — dismissible banners.
- `seo.html` — meta / OG tags.
- `sw-register.html` — service-worker registration snippet.

Global model attributes for every page: `NavModelAdvice`,
`SiteModelAdvice`, plus `OnboardingChecklist`/`OnboardingNudge`
injected by `OnboardingService`.

## Rule

Any new website page **must** call the same service method a future
mobile client would call — no business logic in a controller body, no
Thymeleaf-only shortcut. That is the only way the mobile client and
the website can share behaviour once the JSON API is stood up.
