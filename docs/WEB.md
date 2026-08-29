# WEB — Thymeleaf-rendered website

The public website and the authenticated in-app workspace are both
server-rendered Thymeleaf pages served by the same Spring Boot app.
Every page routes through the same service layer the JSON API uses (see
`docs/API.md`) — no business logic in controllers or templates.

## Page inventory

### Marketing / public

| Route            | Controller                | Template               | Notes                                     |
|------------------|---------------------------|------------------------|-------------------------------------------|
| `/`              | `SeoController`           | `landing.html`         | Landing page; video URL wired from env    |
| `/pricing`       | `PricingController`       | `pricing.html`         | Free / Pro / Business tiers               |
| `/demo`          | `DemoController`          | `demo.html`            | Live-ish demo using `DemoConversationService` |
| `/privacy`, `/terms`, `/refund` | `LegalController` | `legal/*.html`  | Reads Markdown from `src/main/resources/legal/` |

### Auth

| Route            | Controller                | Notes                                     |
|------------------|---------------------------|-------------------------------------------|
| `/login`         | `AuthController`          | Form login + "Continue with Google"       |
| `/register`      | `AuthController`          | Registration form                         |
| `/verify-email`  | `EmailVerificationController` | Token click-through                   |
| `/password/forgot`, `/password/reset` | `PasswordResetController` | Reset flow          |
| `/auth/google/start` | `OAuthStartController` | Google OIDC entry                        |
| `/account`       | `AccountController`       | Profile, billing summary, appearance      |

### App

| Route                  | Controller                  | Notes                                              |
|------------------------|-----------------------------|----------------------------------------------------|
| `/chats`               | `ConversationController`    | Chats list + open pane. **Shares** `ConversationListService` with `/api/v1/chats` |
| `/chats/{key}`         | `ConversationController`    | Single conversation timeline + compose             |
| `/chats/events`        | `ChatEventsController`      | SSE stream (`NewMailEvent`) — see fragments/live-refresh |
| `/mailboxes`           | `MailboxController`         | Connect / disconnect IMAP + Gmail                  |
| `/mailboxes/gmail/*`   | `GmailConnectController`    | Google OAuth for Gmail read/write                  |
| `/saved-searches`      | `SavedSearchController`     | CRUD saved searches                                |
| `/digest`, `/digest/opt-out` | `DigestController`    | Weekly digest preferences                          |

### Billing

| Route                  | Controller                  | Notes                                              |
|------------------------|-----------------------------|----------------------------------------------------|
| `/billing/*`           | `BillingController`         | Checkout, portal, cancel confirm                   |
| `/billing/webhook`     | `BillingWebhookController`  | Stripe webhook (CSRF-exempt; signature verified)   |

### Admin

| Route              | Controller               | Notes                                     |
|--------------------|--------------------------|-------------------------------------------|
| `/admin/revenue`   | `AdminRevenueController` | MRR, churn, at-risk queue, win-back CTA   |

### PWA

| Route              | Controller               | Notes                                     |
|--------------------|--------------------------|-------------------------------------------|
| `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/**` | `PwaController` | PWA shell |

## Shared fragments and layouts

- `templates/layout.html` — site chrome, top nav, footer.
- `templates/fragments/nav.html`, `fragments/site-nav.html` — nav bar
  (in-app vs. marketing variants), model populated by
  `NavModelAdvice` and `SiteModelAdvice`.
- `templates/fragments/live-refresh.html` — the SSE client that morphs
  the rail and the open pane in place when a `NewMailEvent` arrives.
- `templates/fragments/chat-bubbles.html`, `fragments/rail-row.html` —
  reused by the full page and the SSE refresh fragments so the
  markup never diverges.

## Web ↔ API parity rule

When a page needs a new server-side capability, add it in the service
layer first and consume it from BOTH `web/*Controller` and
`api/*Controller`. If the mobile client needs a shape the website
doesn't render, add a small mapper in `api/`, but never fork the query
or the business rule.
