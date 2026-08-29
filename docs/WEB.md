# Website (Thymeleaf)

The public-facing site + authenticated app UI, all rendered server-side
from `src/main/resources/templates/`. Every page's business logic runs
in the SAME service beans the (planned) mobile REST API uses — never
duplicated.

## Layout

- `templates/*.html` — top-level pages.
- `templates/fragments/*.html` — reusable Thymeleaf fragments.
- `templates/admin/`, `billing/`, `digest/`, `mailboxes/`, `password/`,
  `team/` — subtrees keyed to feature area.
- `static/css/`, `static/js/`, `static/images/` — assets.

## Page inventory

Marketing / auth (public):

| Path                  | Template              | Controller                       |
|-----------------------|-----------------------|----------------------------------|
| `/`                   | `landing.html`        | `web.SeoController`              |
| `/pricing`            | `pricing.html`        | `web.PricingController`          |
| `/demo`               |                       | `web.DemoController`             |
| `/legal/*`            | `legal.html`          | `web.LegalController`            |
| `/login`, `/register` | `login.html`, `register.html` | `auth.AuthController`    |
| `/password/forgot`, `/password/reset` | `password/*.html` | `auth.PasswordResetController` |
| `/verify-email`       | `verify-email.html`   | `auth.EmailVerificationController` |

App (authenticated):

| Path                       | Template                              | Controller                        |
|----------------------------|---------------------------------------|-----------------------------------|
| `/chats`, `/chats/{key}`   | `chat.html` (+ `fragments/chat-pane`) | `web.ConversationController`      |
| `/chats/{key}/pane`        | `fragments/chat-pane :: pane`         | `web.ConversationController`      |
| `/chats/new`               | `compose.html`                        | `web.ConversationController`      |
| `/chats/events` (SSE)      | —                                     | `web.ChatEventsController`        |
| `/chats/refresh` (JSON)    | —                                     | `web.ConversationController` (foreground heartbeat) |
| `/mailboxes/*`             | `mailboxes/*.html`                    | `web.MailboxController`, `GmailConnectController` |
| `/saved-searches/*`        | `savedsearches/*`                     | `web.SavedSearchController`       |
| `/account*`                | `account*.html`                       | `auth.AccountController`          |
| `/billing/*`               | `billing/*.html`                      | `billing.BillingController`       |
| `/team/*`                  | `team/*.html`                         | `team.*Controller`                |
| `/digest/opt-out`          | `digest/*.html`                       | `digest.DigestController`         |
| `/admin/*`                 | `admin/*.html`                        | `admin.AdminRevenueController`    |
| `/manifest.webmanifest`, `/sw.js`, `/offline` | — / `pwa/*`         | `web.PwaController`               |

## Shared fragments

- `fragments/layout.html` — page shell (head, header, nav, footer).
- `fragments/chat-pane.html` — the open conversation's timeline + reply
  box; reused by `chat.html` for a full render and by
  `ConversationController#chatPane` for an in-place morph.
- `fragments/live-refresh.html` — SSE wiring that redraws the rail and
  the open pane when `/chats/events` announces new mail.
- `fragments/nav.html`, `fragments/billing-banner.html`,
  `fragments/onboarding-nudge.html` — reused across app pages.

## Rules

- The website's JSON helpers (`/chats/refresh`) do not count as a
  stable API — they exist for the web page's own AJAX. The mobile
  client MUST call the versioned surface (`/api/v1/*`) instead.
- All state mutations happen in a `@Transactional` service method, not
  in a controller. Controllers are thin: parse input → call service →
  render or redirect.
