# Web (Thymeleaf) inventory

Server-rendered pages under `src/main/resources/templates/`, driven by
controllers in `com.emailmessenger.web` and `com.emailmessenger.auth`.
Every route below shares the service layer with the (in-progress) REST
API — see `docs/API.md` and `docs/BACKEND.md`.

## Route → controller → template

### Public / marketing

| Route | Controller | Template | Notes |
| --- | --- | --- | --- |
| `GET /` | `LandingController` (see `web/`) | `landing.html` | Marketing landing. |
| `GET /pricing` | `PricingController` | `pricing.html` | Plan comparison. |
| `GET /demo` | `DemoController` | `chat.html` (demo mode) | Interactive product demo, no login. |
| `GET /privacy`, `/terms`, `/refund` | `LegalController` | `legal.html` | Legal pages. |
| `GET /robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` | inline / static | SEO surface. |

### Auth

| Route | Controller | Template |
| --- | --- | --- |
| `GET /login` | `AuthController` | `login.html` |
| `POST /login` | Spring Security (`SecurityConfig`) | — |
| `GET /register`, `POST /register` | `AuthController` | `register.html` |
| `GET /verify-email` | `EmailVerificationController` | `verify-email.html` |
| `POST /verify-email/resend` | `EmailVerificationController` | — |
| `GET /auth/google/start` | `OAuthStartController` | redirect to Google OIDC |
| `GET /password/forgot`, `POST /password/forgot` | `PasswordResetController` | `password/forgot.html` |
| `GET /password/reset`, `POST /password/reset` | `PasswordResetController` | `password/reset.html` |
| `POST /logout` | Spring Security | — |

### Account

| Route | Controller | Template |
| --- | --- | --- |
| `GET /account` | `AccountController` | `account.html` |
| `GET /account/notifications` | `AccountController` | `account-notifications.html` |
| `GET /account/appearance`, `POST /account/appearance` | `AccountController` | `account-appearance.html` |
| `GET /account/privacy` | `AccountController` | `account-privacy.html` |
| `POST /account/password`, `POST /account/email` | `AccountController` | — |

### Mailboxes

| Route | Controller | Template |
| --- | --- | --- |
| `GET /mailboxes`, `GET /mailboxes/new` | `MailboxController` | `mailboxes/*.html` |
| `POST /mailboxes`, `POST /mailboxes/{id}/sync`, `POST /mailboxes/{id}/signature`, `POST /mailboxes/{id}/delete` | `MailboxController` | — |
| `POST /mailboxes/preferences` | `MailboxController` | — |
| `GET /mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` | redirects |

### Chats (the workspace)

| Route | Controller | Template |
| --- | --- | --- |
| `GET /chats` | `ChatRedirectController` → `ConversationController` | `chat.html` |
| `GET /chats/new`, `POST /chats/new` | `ConversationController` | `compose.html` / redirect |
| `POST /chats/refresh` | `ConversationController` | — |
| `GET /chats/{key}`, `GET /chats/{key}/pane` | `ConversationController` | `chat.html` / fragment |
| `POST /chats/{key}/send` | `ConversationController` | — |
| `GET /chats/events` (SSE) | `ChatEventsController` (`ChatEventStream`) | text/event-stream |
| `GET /threads/**`, `GET /senders` | `ChatRedirectController` | redirect (legacy URLs) |

### Saved searches

| Route | Controller | Template |
| --- | --- | --- |
| `POST /searches`, `POST /searches/{id}/delete` | `SavedSearchController` | — |

### Digests

| Route | Controller |
| --- | --- |
| `GET /digest/opt-out` | `DigestController` |

### Admin (guarded by `AdminAuthorizer`)

| Route | Controller |
| --- | --- |
| `GET /admin/revenue`, various admin metrics dashboards | `AdminRevenueController` and friends under `admin/` |

### PWA

| Route | Controller |
| --- | --- |
| `GET /manifest.webmanifest`, `/icons/*.png`, `/apple-touch-icon.png` | `PwaController` |
| `GET /sw.js`, `/offline` | `PwaController` |

### Billing (Stripe)

| Route | Controller | Notes |
| --- | --- | --- |
| Checkout / portal redirects | `BillingController` | Stripe hosted UI. |
| `POST /billing/webhook` | `BillingWebhookController` | `@RestController`, CSRF-exempt. |

## Fragments and layouts

- `templates/fragments/` — shared header/nav/footer, live-refresh
  hooks, upgrade modal, onboarding checklist.
- `NavModelAdvice` and `SiteModelAdvice` (in `web/`) populate global
  model attributes for every rendered page.

## Rule of one service layer

Every controller listed above is a thin adapter over `service/`,
`auth/`, `email/`, or `billing/`. When a REST controller is added for
the mobile client (see `docs/API.md`), it MUST route through the same
service classes — never fork logic into a REST-only path.
