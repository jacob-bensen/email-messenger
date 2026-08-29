# Thymeleaf Website

The public-facing website served by this same Spring Boot app. Templates
live under `src/main/resources/templates/`; the CSS lives under
`src/main/resources/static/css/`. Controllers live under
`com.emailmessenger.web`.

REST for the mobile client lives under `/api/v1/**` and is documented in
`docs/API.md`. Both go through the SAME service layer — do not duplicate
logic between the two paths.

## Page inventory

| Route | Template | Controller | Notes |
| --- | --- | --- | --- |
| `/` | `landing.html` | `LandingProperties` + a controller in `web/` | Marketing landing page. |
| `/pricing` | `pricing.html` | `PricingController` | Free / Pro / Business tiers. |
| `/demo` | (in `web/`) | `DemoController` | Interactive product demo. |
| `/login`, `/register` | `login.html`, `register.html` | `AuthController` | Form login + "Continue with Google". |
| `/password/forgot`, `/password/reset` | `password/*.html` | `PasswordResetController` | Password reset. |
| `/verify-email` | `verify-email.html` | `EmailVerificationController` | Email address verification. |
| `/chats` | `chat.html` | `ChatRedirectController` → workspace | Texting-style workspace keyed by `conversationKey`. |
| `/chats/events` (SSE) | — | `ChatEventStream`, `ChatEventsController` | Live rail/pane updates (web only). |
| `/conversations/{id}` | `conversation.html` | `ConversationController` | Legacy per-thread view. |
| `/compose` | `compose.html` | (in `web/`) | New conversation flow. |
| `/mailboxes/**` | `mailboxes/*.html` | `MailboxController`, `GmailConnectController` | Connect IMAP + Gmail OAuth. |
| `/account/**` | `account*.html` | `AccountController` | Plan, notifications, appearance, privacy. |
| `/billing/**` | `billing/*.html` | (billing package), `BillingWebhookController` | Stripe checkout + webhook. |
| `/team/**` | `team/*.html` | (team package) | Teams & invites. |
| `/admin/**` | `admin/*.html` | (admin package) | Admin metrics. |
| `/digest/**` | `digest/*.html` | (digest package) | Daily / weekly digest opt-in. |
| `/privacy`, `/terms`, `/refund` | `legal.html` | `LegalController` | Legal pages. |
| `/manifest.webmanifest`, `/sw.js`, `/offline` | — | `PwaController` | PWA shell. |
| `/robots.txt`, `/sitemap.xml`, `/.well-known/**` | — | `SeoController` | SEO scaffolding. |

## Shared fragments

Under `templates/fragments/`:

- `live-refresh.html` — wires web pages up to the `/chats/events` SSE
  stream (used by `chat.html`).
- Other fragments follow the convention of one file per reusable block
  and are included via `th:insert`/`th:replace`.

## Services touched by pages

- `ChatService` — the texting-style workspace (rail + open pane).
- `ConversationListService` — the rail (chats list).
- `ConversationController` uses `ChatService`; the future
  `GET /api/v1/conversations/**` REST endpoints MUST also use
  `ChatService` — no logic fork.
- `OutboundMessageService` — SMTP send path for both the web reply form
  and the future `POST /api/v1/conversations/{key}/messages` REST route.
- `SavedSearchService`, `SavedSearchCountService` — saved-search UI on
  web; mirror in the mobile roadmap.

## Styling & PWA

- CSS lives under `static/css/`. Dark mode is CSS-driven; the same theme
  should inform the mobile client's palette.
- The web app is a PWA (`sw.js`, `manifest.webmanifest`,
  `apple-touch-icon.png`). Push notifications for the web are on the
  backend roadmap as "Live delivery, part 2".
